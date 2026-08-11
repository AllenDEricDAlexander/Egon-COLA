package top.egon.cola.component.gateway.engine.rpc;

import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 维护 Gateway RPC Slot 的监听、准入注册、心跳续约和排空状态。
 * / Maintains listening, admitted registration, heartbeat renewal, and draining state for the Gateway RPC slot.
 */
public final class RpcGatewaySlotRuntime implements AutoCloseable {

    private final DdcServiceRegistryClient registry;

    private final DdcServiceKeyFactory serviceKeyFactory;

    private final RpcGatewaySlotProperties properties;

    /** 为每次注册和心跳提供精确绑定的短期票据。 / Supplies an exactly bound short-lived ticket for every registration and heartbeat. */
    private final DdcAdmissionTicketSupplier admissionTickets;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<RpcGatewaySubsystemState> state;

    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    private volatile DdcLeaseSession lease;

    private volatile DdcServiceRegistration registration;

    /** RPC Listener 实际监听端口。 / Actual port bound by the RPC listener. */
    private volatile Integer actualPort;

    private volatile RuntimeException lastFailure;

    /**
     * 创建 Gateway RPC Slot 租约运行时。
     * / Creates the Gateway RPC-slot lease runtime.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param properties RPC Slot 参数 / RPC-slot settings
     * @param admissionTickets 准入票据端口 / admission-ticket port
     */
    public RpcGatewaySlotRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            RpcGatewaySlotProperties properties,
            DdcAdmissionTicketSupplier admissionTickets) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.serviceKeyFactory = Objects.requireNonNull(
                serviceKeyFactory,
                "serviceKeyFactory"
        );
        this.properties = Objects.requireNonNull(properties, "properties");
        this.admissionTickets = Objects.requireNonNull(
                admissionTickets,
                "admissionTickets"
        );
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

    /**
     * 记录 RPC Listener 实际端口，但尚不对外宣告 Ready。
     * / Records the actual RPC-listener port without advertising readiness yet.
     *
     * @param actualPort 实际监听端口 / actual listening port
     */
    public synchronized void listenerStarted(int actualPort) {
        if (!properties.enabled()) {
            return;
        }
        if (actualPort < 1 || actualPort > 65535) {
            throw new IllegalArgumentException("RPC listener port is invalid");
        }
        this.actualPort = actualPort;
        state.set(RpcGatewaySubsystemState.LISTENING_NOT_REGISTERED);
    }

    public synchronized void engineReady() {
        if (!properties.enabled()) {
            return;
        }
        if (actualPort == null) {
            throw new IllegalStateException("RPC listener is not started");
        }
        registerRecoverably();
        scheduleHeartbeat();
    }

    /**
     * 使用新鲜准入票据续约，失租或异常时进入恢复态。
     * / Renews with a fresh admission ticket and enters recovery after lease loss or failure.
     */
    public synchronized void heartbeatAndRecover() {
        RpcGatewaySubsystemState currentState = state.get();
        if (actualPort == null
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
            DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
            request.setServiceKey(registration.serviceKey());
            request.setInstanceId(lease.instanceId());
            request.setLeaseId(lease.leaseId());
            request.setAdmissionTicket(admissionTicket(
                    registration.serviceKey()
            ));
            var result = Objects.requireNonNull(
                    registry.heartbeat(request),
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
            registration = registration();
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

    /**
     * 使用实际端口和新鲜票据构造本轮注册。
     * / Builds the current registration with the actual port and a fresh ticket.
     *
     * @return DDC 服务注册请求 / DDC service registration
     */
    private DdcServiceRegistration registration() {
        DdcServiceKey serviceKey = serviceKeyFactory.fromScope(
                DdcServiceKind.INTERNAL_GATEWAY,
                properties.serviceName(),
                properties.group(),
                properties.version(),
                "grpc"
        );
        return new DdcServiceRegistration(
                properties.instanceId(),
                serviceKey,
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
                        properties.gatewayGroupCode(),
                        "gateway.component",
                        "engine"
                ),
                properties.leaseSeconds(),
                properties.heartbeatIntervalSeconds(),
                admissionTicket(serviceKey)
        );
    }

    /**
     * 为当前 Gateway 实例和精确物理作用域取得原始票据。
     * / Obtains the raw ticket for the current Gateway instance and exact physical scope.
     *
     * @param serviceKey 实际发送的服务键 / service key actually sent
     * @return 原始短期准入 JWT / raw short-lived admission JWT
     */
    private String admissionTicket(DdcServiceKey serviceKey) {
        return admissionTickets.getTicket(
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                properties.instanceId()
        ).value();
    }
}
