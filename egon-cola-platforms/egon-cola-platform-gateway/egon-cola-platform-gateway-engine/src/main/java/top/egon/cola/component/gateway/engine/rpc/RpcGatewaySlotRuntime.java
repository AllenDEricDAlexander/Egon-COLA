package top.egon.cola.component.gateway.engine.rpc;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class RpcGatewaySlotRuntime implements AutoCloseable {

    private final DdcServiceRegistryClient registry;

    private final DdcServiceKeyFactory serviceKeyFactory;

    private final RpcGatewaySlotProperties properties;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<RpcGatewaySubsystemState> state;

    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    private volatile DdcLeaseSession lease;

    private volatile DdcServiceRegistration registration;

    private volatile RuntimeException lastFailure;

    public RpcGatewaySlotRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            RpcGatewaySlotProperties properties) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.serviceKeyFactory = Objects.requireNonNull(
                serviceKeyFactory,
                "serviceKeyFactory"
        );
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
                serviceKeyFactory.fromScope(
                        DdcServiceKind.INTERNAL_GATEWAY,
                        properties.serviceName(),
                        properties.group(),
                        properties.version(),
                        "grpc"
                ),
                properties.advertisedHost(),
                actualPort,
                properties.secure(),
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
        registerRecoverably();
        scheduleHeartbeat();
    }

    public synchronized void heartbeatAndRecover() {
        RpcGatewaySubsystemState currentState = state.get();
        if (registration == null
                || currentState == RpcGatewaySubsystemState.DISABLED
                || currentState == RpcGatewaySubsystemState.DRAINING
                || currentState == RpcGatewaySubsystemState.STOPPED) {
            return;
        }
        if (lease == null
                || currentState == RpcGatewaySubsystemState.RECOVERING) {
            registerRecoverably();
            return;
        }
        if (currentState != RpcGatewaySubsystemState.REGISTERED_READY) {
            return;
        }
        try {
            var result = Objects.requireNonNull(
                    registry.heartbeat(
                            lease.instanceId(),
                            lease.leaseId()
                    ),
                    "heartbeat result"
            );
            if (!result.renewed()) {
                enterRecovery(new IllegalStateException(
                        "RPC Gateway slot lease was not renewed: "
                                + result.status()
                ));
            } else if (result.leaseExpireAt() != null) {
                renewLeaseExpiry(result.leaseExpireAt());
            }
        } catch (RuntimeException failure) {
            enterRecovery(failure);
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

    public Optional<RuntimeException> lastFailure() {
        return Optional.ofNullable(lastFailure);
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
            synchronized (this) {
                enterRecovery(failure);
            }
        }
    }

    private void scheduleHeartbeat() {
        if (!heartbeatScheduled.compareAndSet(false, true)) {
            return;
        }
        scheduler.scheduleWithFixedDelay(
                this::heartbeatSafely,
                properties.heartbeatIntervalSeconds(),
                properties.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void registerRecoverably() {
        try {
            lease = Objects.requireNonNull(
                    registry.register(registration),
                    "registered lease"
            );
            state.set(RpcGatewaySubsystemState.REGISTERED_READY);
        } catch (RuntimeException failure) {
            enterRecovery(failure);
        }
    }

    private void enterRecovery(RuntimeException failure) {
        RpcGatewaySubsystemState currentState = state.get();
        if (currentState == RpcGatewaySubsystemState.DRAINING
                || currentState == RpcGatewaySubsystemState.STOPPED
                || currentState == RpcGatewaySubsystemState.DISABLED) {
            return;
        }
        lease = null;
        lastFailure = failure;
        state.set(RpcGatewaySubsystemState.RECOVERING);
    }

    private void renewLeaseExpiry(Instant leaseExpireAt) {
        DdcLeaseSession current = lease;
        lease = new DdcLeaseSession(
                current.instanceId(),
                current.leaseId(),
                current.role(),
                current.leaseSeconds(),
                current.heartbeatIntervalSeconds(),
                current.registeredAt(),
                leaseExpireAt
        );
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
