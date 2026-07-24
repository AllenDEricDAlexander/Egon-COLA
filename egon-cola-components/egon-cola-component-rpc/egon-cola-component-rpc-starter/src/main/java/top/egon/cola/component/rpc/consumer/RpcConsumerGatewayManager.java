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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RpcConsumerGatewayManager implements SmartLifecycle {

    private final Object monitor = new Object();

    private final DdcServiceRegistryClient registryClient;

    private final RpcConsumerChannelFactory channelFactory;

    private final EgonRpcProperties.Consumer properties;

    private final DdcServiceKey gatewayServiceKey;

    private final ScheduledExecutorService drainExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "egon-rpc-consumer-channel-drain"
                );
                thread.setDaemon(true);
                return thread;
            });

    private volatile RpcGatewayState state = RpcGatewayState.STOPPED;

    private volatile ActiveGateway activeGateway;

    private volatile DdcRegistrySubscription subscription;

    public RpcConsumerGatewayManager(
            DdcServiceRegistryClient registryClient,
            RpcConsumerChannelFactory channelFactory,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        this.registryClient = registryClient;
        this.channelFactory = channelFactory;
        this.properties = properties.getConsumer();
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
            state = RpcGatewayState.STARTING;
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
        synchronized (monitor) {
            state = RpcGatewayState.STOPPED;
            if (subscription != null) {
                subscription.close();
                subscription = null;
            }
            closeNow(activeGateway);
            activeGateway = null;
            monitor.notifyAll();
        }
        drainExecutor.shutdownNow();
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
                    .map(RpcGatewayEndpoint::from)
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

    private void drain(ActiveGateway gateway) {
        if (gateway == null) {
            return;
        }
        gateway.channel().shutdown();
        drainExecutor.schedule(
                gateway.channel()::shutdownNow,
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
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        closeNow(activeGateway);
        activeGateway = null;
        state = RpcGatewayState.STOPPED;
        drainExecutor.shutdownNow();
        throw new EgonRpcException(code, "RPC Gateway discovery failed");
    }

    private record ActiveGateway(
            RpcGatewayEndpoint endpoint,
            ManagedChannel channel
    ) {
    }
}
