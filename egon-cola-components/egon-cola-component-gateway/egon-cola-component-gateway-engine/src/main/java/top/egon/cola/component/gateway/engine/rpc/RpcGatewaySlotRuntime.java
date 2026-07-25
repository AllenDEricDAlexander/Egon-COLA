package top.egon.cola.component.gateway.engine.rpc;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class RpcGatewaySlotRuntime implements AutoCloseable {

    private final DdcServiceRegistryClient registry;

    private final RpcGatewaySlotProperties properties;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<RpcGatewaySubsystemState> state;

    private volatile DdcLeaseSession lease;

    private volatile DdcServiceRegistration registration;

    public RpcGatewaySlotRuntime(
            DdcServiceRegistryClient registry,
            RpcGatewaySlotProperties properties) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.properties = Objects.requireNonNull(properties, "properties");
        state = new AtomicReference<>(properties.enabled()
                ? RpcGatewaySubsystemState.STARTING
                : RpcGatewaySubsystemState.DISABLED);
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "gateway-rpc-slot-lease"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void listenerStarted(int actualPort) {
        if (!properties.enabled()) {
            return;
        }
        if (actualPort < 1 || actualPort > 65535) {
            throw new IllegalArgumentException("RPC listener port is invalid");
        }
        registration = new DdcServiceRegistration(
                properties.instanceId(),
                new DdcServiceKey(
                        properties.env(),
                        properties.namespace(),
                        DdcServiceKind.INTERNAL_GATEWAY,
                        properties.serviceName(),
                        properties.group(),
                        properties.version(),
                        "grpc"
                ),
                properties.advertisedHost(),
                actualPort,
                false,
                Map.of(
                        "egon.rpc.transport", "grpc",
                        "egon.rpc.serialization", "protobuf",
                        "egon.rpc.runtime-version",
                        properties.rpcRuntimeVersion(),
                        "gateway.engine-version",
                        properties.gatewayVersion(),
                        "gateway.group-code",
                        properties.gatewayGroupCode()
                ),
                properties.leaseSeconds(),
                properties.heartbeatIntervalSeconds()
        );
        state.set(RpcGatewaySubsystemState.LISTENING_NOT_REGISTERED);
    }

    public synchronized void engineReady() {
        if (!properties.enabled()) {
            return;
        }
        if (registration == null) {
            throw new IllegalStateException("RPC listener is not started");
        }
        register();
        scheduler.scheduleWithFixedDelay(
                this::heartbeatSafely,
                properties.heartbeatIntervalSeconds(),
                properties.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    public synchronized void heartbeatAndRecover() {
        if (state.get() != RpcGatewaySubsystemState.REGISTERED_READY) {
            return;
        }
        if (lease == null || !registry.heartbeat(
                lease.instanceId(),
                lease.leaseId()
        ).renewed()) {
            register();
        }
    }

    public synchronized void beginDrain() {
        if (!properties.enabled()
                || state.get() == RpcGatewaySubsystemState.STOPPED) {
            return;
        }
        state.set(RpcGatewaySubsystemState.DRAINING);
        scheduler.shutdownNow();
        deregister();
    }

    public RpcGatewaySubsystemState state() {
        return state.get();
    }

    public Optional<DdcLeaseSession> lease() {
        return Optional.ofNullable(lease);
    }

    @Override
    public synchronized void close() {
        beginDrain();
        state.set(RpcGatewaySubsystemState.STOPPED);
    }

    private void heartbeatSafely() {
        try {
            heartbeatAndRecover();
        } catch (RuntimeException failure) {
            state.set(RpcGatewaySubsystemState.FAILED);
        }
    }

    private void register() {
        lease = registry.register(registration);
        state.set(RpcGatewaySubsystemState.REGISTERED_READY);
    }

    private void deregister() {
        DdcLeaseSession current = lease;
        lease = null;
        if (current != null) {
            try {
                registry.deregister(
                        current.instanceId(),
                        current.leaseId()
                );
            } catch (RuntimeException ignored) {
                // DDC TTL eventually removes an unclean slot lease.
            }
        }
    }
}
