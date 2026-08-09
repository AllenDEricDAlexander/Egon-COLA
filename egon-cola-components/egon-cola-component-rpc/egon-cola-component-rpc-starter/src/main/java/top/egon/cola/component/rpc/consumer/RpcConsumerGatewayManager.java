package top.egon.cola.component.rpc.consumer;

import io.grpc.ManagedChannel;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceInstance;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.model.DdcServiceSnapshot;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RpcConsumerGatewayManager implements SmartLifecycle {

    private final Object monitor = new Object();

    private final DdcServiceRegistryClient registryClient;

    private final RpcConsumerChannelFactory channelFactory;

    private final EgonRpcProperties.Consumer properties;

    private final DdcServiceKey gatewayServiceKey;

    private final Set<ManagedChannel> drainingChannels =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private final AtomicLong roundRobinSequence = new AtomicLong();

    private volatile RpcGatewayState state = RpcGatewayState.STOPPED;

    private volatile List<ActiveGateway> activeGateways = List.of();

    private volatile DdcRegistrySubscription subscription;

    private volatile ScheduledExecutorService drainExecutor;

    public RpcConsumerGatewayManager(
            DdcServiceRegistryClient registryClient,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            DdcServiceKeyFactory serviceKeyFactory) {
        this.registryClient = registryClient;
        this.channelFactory = channelFactory;
        this.properties = properties.getConsumer();
        validateProperties();
        this.gatewayServiceKey = gatewayServiceKey(
                processIdentity,
                serviceKeyFactory
        );
    }

    private DdcServiceKey gatewayServiceKey(
            RpcProcessIdentity processIdentity,
            DdcServiceKeyFactory serviceKeyFactory) {
        String bizCode = properties.getGatewayBizCode();
        String appCode = properties.getGatewayAppCode();
        if (blank(bizCode) && blank(appCode)) {
            return serviceKeyFactory.fromScope(
                    processIdentity.env(),
                    DdcServiceKind.INTERNAL_GATEWAY,
                    properties.getGatewayServiceName(),
                    properties.getGatewayGroup(),
                    properties.getGatewayVersion(),
                    "grpc"
            );
        }
        if (blank(bizCode) || blank(appCode)) {
            throw new IllegalArgumentException(
                    "RPC Gateway biz-code and app-code must be configured together"
            );
        }
        return serviceKeyFactory.fromTargetScope(
                bizCode,
                appCode,
                processIdentity.env(),
                DdcServiceKind.INTERNAL_GATEWAY,
                properties.getGatewayServiceName(),
                properties.getGatewayGroup(),
                properties.getGatewayVersion(),
                "grpc"
        );
    }

    @Override
    public void start() {
        synchronized (monitor) {
            if (state != RpcGatewayState.STOPPED) {
                return;
            }
            validateProperties();
            state = RpcGatewayState.STARTING;
            drainExecutor = newDrainExecutor();
            try {
                subscription = registryClient.subscribe(
                        gatewayServiceKey,
                        this::acceptSnapshot
                );
                drainExecutor.scheduleWithFixedDelay(
                        this::expireGateways,
                        100,
                        100,
                        TimeUnit.MILLISECONDS
                );
            } catch (RuntimeException exception) {
                cleanupStartup();
                throw new EgonRpcException(
                        EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE,
                        "RPC Gateway discovery failed",
                        exception
                );
            }
            long deadline = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(
                            properties.getGatewayDiscoveryTimeoutMs()
                    );
            while (state == RpcGatewayState.STARTING
                    && System.nanoTime() < deadline) {
                try {
                    long remainingMs = Math.max(
                            1,
                            TimeUnit.NANOSECONDS.toMillis(
                                    deadline - System.nanoTime()
                            )
                    );
                    monitor.wait(remainingMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failStartup();
                }
            }
            if (state != RpcGatewayState.READY) {
                failStartup();
            }
        }
    }

    @Override
    public void stop() {
        ScheduledExecutorService executor;
        synchronized (monitor) {
            state = RpcGatewayState.STOPPED;
            if (subscription != null) {
                subscription.close();
                subscription = null;
            }
            activeGateways.forEach(this::closeNow);
            activeGateways = List.of();
            drainingChannels.forEach(ManagedChannel::shutdownNow);
            drainingChannels.clear();
            executor = drainExecutor;
            drainExecutor = null;
            monitor.notifyAll();
        }
        shutdownExecutor(executor);
    }

    @Override
    public boolean isRunning() {
        return state == RpcGatewayState.READY;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 50;
    }

    public ManagedChannel currentChannel() {
        return currentChannel(Set.of());
    }

    public ManagedChannel currentChannel(Set<ManagedChannel> excluded) {
        expireGateways();
        List<ActiveGateway> candidates = activeGateways.stream()
                .filter(gateway -> !excluded.contains(gateway.channel()))
                .filter(gateway -> !gateway.channel().isShutdown())
                .toList();
        if (state != RpcGatewayState.READY || candidates.isEmpty()) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE,
                    "the RPC Gateway is unavailable"
            );
        }
        int index = Math.floorMod(
                roundRobinSequence.getAndIncrement(),
                candidates.size()
        );
        return candidates.get(index).channel();
    }

    public void recordFailure(ManagedChannel failed) {
        if (failed == null) {
            return;
        }
        synchronized (monitor) {
            List<ActiveGateway> remaining = new ArrayList<>();
            for (ActiveGateway gateway : activeGateways) {
                if (gateway.channel() == failed) {
                    drain(gateway);
                } else {
                    remaining.add(gateway);
                }
            }
            activeGateways = List.copyOf(remaining);
            if (activeGateways.isEmpty()
                    && state != RpcGatewayState.STOPPED) {
                state = RpcGatewayState.UNAVAILABLE;
            }
        }
    }

    public int maxAttempts() {
        return properties.getGatewayMaxAttempts();
    }

    public RpcGatewayState state() {
        return state;
    }

    public RpcGatewayEndpoint endpoint() {
        List<ActiveGateway> current = activeGateways;
        return current.isEmpty() ? null : current.getFirst().endpoint();
    }

    public List<RpcGatewayEndpoint> endpoints() {
        return activeGateways.stream()
                .map(ActiveGateway::endpoint)
                .toList();
    }

    private void acceptSnapshot(DdcServiceSnapshot snapshot) {
        synchronized (monitor) {
            if (state == RpcGatewayState.STOPPED) {
                return;
            }
            Instant now = Instant.now();
            List<RpcGatewayEndpoint> desired = snapshot.instances().stream()
                    .filter(instance -> isUp(instance, now))
                    .map(this::validEndpoint)
                    .flatMap(Optional::stream)
                    .filter(endpoint -> endpoint.activeAt(now))
                    .sorted(Comparator
                            .comparing(RpcGatewayEndpoint::instanceId)
                            .thenComparing(RpcGatewayEndpoint::leaseId))
                    .toList();
            reconcile(desired);
            if (!activeGateways.isEmpty()) {
                state = RpcGatewayState.READY;
            } else if (state != RpcGatewayState.STARTING) {
                state = RpcGatewayState.UNAVAILABLE;
            }
            monitor.notifyAll();
        }
    }

    private void reconcile(List<RpcGatewayEndpoint> desired) {
        Map<GatewayIdentity, ActiveGateway> existing =
                new LinkedHashMap<>();
        activeGateways.forEach(gateway -> existing.put(
                GatewayIdentity.from(gateway.endpoint()),
                gateway
        ));
        List<ActiveGateway> next = new ArrayList<>();
        for (RpcGatewayEndpoint endpoint : desired) {
            GatewayIdentity identity = GatewayIdentity.from(endpoint);
            ActiveGateway retained = existing.remove(identity);
            if (retained != null && !retained.channel().isShutdown()) {
                next.add(new ActiveGateway(endpoint, retained.channel()));
                continue;
            }
            ActiveGateway connected = connect(endpoint);
            if (connected != null) {
                next.add(connected);
            }
        }
        existing.values().forEach(this::drain);
        activeGateways = List.copyOf(next);
    }

    private ActiveGateway connect(RpcGatewayEndpoint endpoint) {
        ManagedChannel channel = channelFactory.create(endpoint);
        if (!channelFactory.awaitReady(
                channel,
                properties.getGatewayDiscoveryTimeoutMs()
        )) {
            channel.shutdownNow();
            return null;
        }
        return new ActiveGateway(endpoint, channel);
    }

    private boolean isUp(DdcServiceInstance instance, Instant now) {
        return instance.normalizedStatus().isAvailable(
                now,
                instance.leaseExpireAt()
        );
    }

    private Optional<RpcGatewayEndpoint> validEndpoint(
            DdcServiceInstance instance) {
        try {
            return Optional.of(RpcGatewayEndpoint.from(instance));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void drain(ActiveGateway gateway) {
        if (gateway == null) {
            return;
        }
        ManagedChannel channel = gateway.channel();
        ScheduledExecutorService executor = drainExecutor;
        if (executor == null) {
            channel.shutdownNow();
            return;
        }
        drainingChannels.add(channel);
        channel.shutdown();
        executor.schedule(
                () -> {
                    synchronized (monitor) {
                        drainingChannels.remove(channel);
                    }
                    channel.shutdownNow();
                },
                properties.getChannelDrainTimeoutMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void closeNow(ActiveGateway gateway) {
        if (gateway != null) {
            gateway.channel().shutdownNow();
        }
    }

    private void expireGateways() {
        synchronized (monitor) {
            Instant now = Instant.now();
            List<ActiveGateway> retained = new ArrayList<>();
            for (ActiveGateway gateway : activeGateways) {
                if (gateway.endpoint().activeAt(now)
                        && !gateway.channel().isShutdown()) {
                    retained.add(gateway);
                } else {
                    drain(gateway);
                }
            }
            if (retained.size() != activeGateways.size()) {
                activeGateways = List.copyOf(retained);
                if (retained.isEmpty()
                        && state != RpcGatewayState.STOPPED) {
                    state = RpcGatewayState.UNAVAILABLE;
                }
                monitor.notifyAll();
            }
        }
    }

    private void failStartup() {
        cleanupStartup();
        throw new EgonRpcException(
                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE,
                "RPC Gateway discovery failed"
        );
    }

    private void cleanupStartup() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        activeGateways.forEach(this::closeNow);
        activeGateways = List.of();
        drainingChannels.forEach(ManagedChannel::shutdownNow);
        drainingChannels.clear();
        state = RpcGatewayState.STOPPED;
        ScheduledExecutorService executor = drainExecutor;
        drainExecutor = null;
        shutdownExecutor(executor);
    }

    private void validateProperties() {
        if (properties.getDefaultTimeoutMs() <= 0
                || properties.getGatewayDiscoveryTimeoutMs() <= 0
                || properties.getChannelDrainTimeoutMs() <= 0
                || properties.getGatewayMaxAttempts() <= 0) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC Consumer timeout and attempt settings must be positive"
            );
        }
        if (blank(properties.getGatewayServiceName())
                || blank(properties.getGatewayGroup())
                || blank(properties.getGatewayVersion())) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC Gateway service identity is required"
            );
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ScheduledExecutorService newDrainExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "egon-rpc-consumer-channel-drain"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    private void shutdownExecutor(ScheduledExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private record ActiveGateway(
            RpcGatewayEndpoint endpoint,
            ManagedChannel channel
    ) {
    }

    private record GatewayIdentity(
            String instanceId,
            String leaseId,
            String host,
            int port,
            boolean secure
    ) {

        private static GatewayIdentity from(RpcGatewayEndpoint endpoint) {
            return new GatewayIdentity(
                    endpoint.instanceId(),
                    endpoint.leaseId(),
                    endpoint.host(),
                    endpoint.port(),
                    endpoint.secure()
            );
        }
    }
}
