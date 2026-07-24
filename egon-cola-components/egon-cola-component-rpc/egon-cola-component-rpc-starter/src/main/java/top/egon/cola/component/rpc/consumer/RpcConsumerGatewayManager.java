package top.egon.cola.component.rpc.consumer;

import io.grpc.ManagedChannel;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RpcConsumerGatewayManager implements SmartLifecycle {

    private final Object monitor = new Object();

    private final DdcServiceRegistryClient registryClient;

    private final RpcConsumerChannelFactory channelFactory;

    private final EgonRpcProperties.Consumer properties;

    private final DdcServiceKey gatewayServiceKey;

    private final Set<ManagedChannel> drainingChannels =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private volatile RpcGatewayState state = RpcGatewayState.STOPPED;

    private volatile ActiveGateway activeGateway;

    private volatile DdcRegistrySubscription subscription;

    private volatile ScheduledExecutorService drainExecutor;

    public RpcConsumerGatewayManager(
            DdcServiceRegistryClient registryClient,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        this.registryClient = registryClient;
        this.channelFactory = channelFactory;
        this.properties = properties.getConsumer();
        validateProperties();
        this.gatewayServiceKey = new DdcServiceKey(
                processIdentity.env(),
                processIdentity.namespace(),
                DdcServiceKind.INTERNAL_GATEWAY,
                this.properties.getGatewayServiceName(),
                this.properties.getGatewayGroup(),
                this.properties.getGatewayVersion(),
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
                        this::expireActiveGateway,
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
                    failStartup(EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
                }
            }
            if (state != RpcGatewayState.READY) {
                EgonRpcErrorCode code = state == RpcGatewayState.AMBIGUOUS
                        ? EgonRpcErrorCode.RPC_GATEWAY_AMBIGUOUS
                        : EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE;
                failStartup(code);
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
            closeNow(activeGateway);
            activeGateway = null;
            // Replaced channels remain owned by this manager until their
            // graceful drain finishes or stop force-closes them.
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
        expireActiveGateway();
        ActiveGateway current = activeGateway;
        if (state == RpcGatewayState.READY
                && current != null
                && current.endpoint().activeAt(Instant.now())) {
            return current.channel();
        }
        if (state == RpcGatewayState.AMBIGUOUS) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_GATEWAY_AMBIGUOUS,
                    "multiple active RPC Gateways were discovered"
            );
        }
        throw new EgonRpcException(
                EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE,
                "the RPC Gateway is unavailable"
        );
    }

    public RpcGatewayState state() {
        return state;
    }

    public RpcGatewayEndpoint endpoint() {
        ActiveGateway current = activeGateway;
        return current == null ? null : current.endpoint();
    }

    private void acceptSnapshot(DdcServiceSnapshot snapshot) {
        synchronized (monitor) {
            if (state == RpcGatewayState.STOPPED) {
                return;
            }
            List<RpcGatewayEndpoint> endpoints = snapshot.instances().stream()
                    .filter(this::isUp)
                    .map(this::validEndpoint)
                    .flatMap(Optional::stream)
                    .filter(endpoint -> endpoint.activeAt(Instant.now()))
                    .toList();
            if (endpoints.size() != 1) {
                if (endpoints.isEmpty()
                        && state == RpcGatewayState.STARTING) {
                    monitor.notifyAll();
                    return;
                }
                closeNow(activeGateway);
                activeGateway = null;
                state = endpoints.isEmpty()
                        ? RpcGatewayState.UNAVAILABLE
                        : RpcGatewayState.AMBIGUOUS;
                monitor.notifyAll();
                return;
            }
            replace(endpoints.getFirst());
            monitor.notifyAll();
        }
    }

    private void replace(RpcGatewayEndpoint endpoint) {
        ActiveGateway current = activeGateway;
        if (current != null && current.endpoint().equals(endpoint)) {
            state = RpcGatewayState.READY;
            return;
        }
        ManagedChannel replacement = channelFactory.create(endpoint);
        if (!channelFactory.awaitReady(
                replacement,
                properties.getGatewayDiscoveryTimeoutMs()
        )) {
            replacement.shutdownNow();
            if (current != null
                    && current.endpoint().activeAt(Instant.now())) {
                state = RpcGatewayState.READY;
            } else {
                closeNow(current);
                activeGateway = null;
                state = RpcGatewayState.UNAVAILABLE;
            }
            return;
        }
        activeGateway = new ActiveGateway(endpoint, replacement);
        state = RpcGatewayState.READY;
        drain(current);
    }

    private boolean isUp(DdcServiceInstance instance) {
        return instance.status() == null
                || "UP".equalsIgnoreCase(instance.status());
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

    private void expireActiveGateway() {
        synchronized (monitor) {
            ActiveGateway current = activeGateway;
            if (current != null
                    && !current.endpoint().activeAt(Instant.now())) {
                closeNow(current);
                activeGateway = null;
                if (state != RpcGatewayState.STOPPED) {
                    state = RpcGatewayState.UNAVAILABLE;
                }
                monitor.notifyAll();
            }
        }
    }

    private void failStartup(EgonRpcErrorCode code) {
        cleanupStartup();
        throw new EgonRpcException(code, "RPC Gateway discovery failed");
    }

    private void cleanupStartup() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        closeNow(activeGateway);
        activeGateway = null;
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
                || properties.getChannelDrainTimeoutMs() <= 0) {
            throw new EgonRpcException(
                    EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                    "RPC Consumer timeout settings must be positive"
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
}
