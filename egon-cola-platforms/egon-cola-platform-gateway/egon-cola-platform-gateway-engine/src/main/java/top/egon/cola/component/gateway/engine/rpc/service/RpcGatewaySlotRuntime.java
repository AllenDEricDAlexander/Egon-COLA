package top.egon.cola.component.gateway.engine.rpc.service;

import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySlotProperties;
import top.egon.cola.component.gateway.engine.rpc.domain.RpcGatewaySubsystemState;

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
 * 补充说明 / Supplementary summary: {@code RpcGatewaySlotRuntime} 是运行时组件，位于当前 Gateway 模块的相关包中，负责Rpc网关槽位运行时相关的职责与边界。
 * English supplement: {@code RpcGatewaySlotRuntime} is a rpc gateway slot runtime runtime in the current Gateway module; it owns the rpc gateway slot runtime-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcGatewaySlotRuntime implements AutoCloseable {

    /**
     * 中文说明：保存 注册表 对应的状态、依赖或配置值；字段类型为 {@code DdcServiceRegistryClient}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registry; its type is {@code DdcServiceRegistryClient}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcServiceRegistryClient registry;

    /**
     * 中文说明：保存 服务键工厂 对应的状态、依赖或配置值；字段类型为 {@code DdcServiceKeyFactory}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service key factory; its type is {@code DdcServiceKeyFactory}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcServiceKeyFactory serviceKeyFactory;

    /**
     * 中文说明：保存 properties 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewaySlotProperties}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by properties; its type is {@code RpcGatewaySlotProperties}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewaySlotProperties properties;

    /**
     * 为每次注册和心跳提供精确绑定的短期票据。 / Supplies an exactly bound short-lived ticket for every registration and heartbeat.
     * 补充说明 / Supplementary summary: 保存 准入Tickets 对应的状态、依赖或配置值；字段类型为 {@code DdcAdmissionTicketSupplier}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by admission tickets; its type is {@code DdcAdmissionTicketSupplier}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcAdmissionTicketSupplier admissionTickets;

    /**
     * 中文说明：保存 scheduler 对应的状态、依赖或配置值；字段类型为 {@code ScheduledExecutorService}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by scheduler; its type is {@code ScheduledExecutorService}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<RpcGatewaySubsystemState>}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code AtomicReference<RpcGatewaySubsystemState>}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<RpcGatewaySubsystemState> state;

    /**
     * 中文说明：保存 heartbeatScheduled 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by heartbeat scheduled; its type is {@code AtomicBoolean}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    /**
     * 中文说明：保存 租约 对应的状态、依赖或配置值；字段类型为 {@code DdcLeaseSession}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by lease; its type is {@code DdcLeaseSession}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile DdcLeaseSession lease;

    /**
     * 中文说明：保存 registration 对应的状态、依赖或配置值；字段类型为 {@code DdcServiceRegistration}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registration; its type is {@code DdcServiceRegistration}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile DdcServiceRegistration registration;

    /**
     * RPC Listener 实际监听端口。 / Actual port bound by the RPC listener.
     * 补充说明 / Supplementary summary: 保存 actualPort 对应的状态、依赖或配置值；字段类型为 {@code Integer}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by actual port; its type is {@code Integer}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile Integer actualPort;

    /**
     * 中文说明：保存 lastFailure 对应的状态、依赖或配置值；字段类型为 {@code RuntimeException}，由 {@code RpcGatewaySlotRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by last failure; its type is {@code RuntimeException}, and {@code RpcGatewaySlotRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewaySlotRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewaySlotRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile RuntimeException lastFailure;

    /**
     * 创建 Gateway RPC Slot 租约运行时。
     * / Creates the Gateway RPC-slot lease runtime.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param properties RPC Slot 参数 / RPC-slot settings
     * @param admissionTickets 准入票据端口 / admission-ticket port
     * 补充说明 / Supplementary summary: 创建 {@code RpcGatewaySlotRuntime} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English supplement: Creates an instance of {@code RpcGatewaySlotRuntime} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
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
     * 补充说明 / Supplementary summary: 执行 监听器Started 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the listener started operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.listenerStarted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：执行 引擎Ready 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the engine ready operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.engineReady(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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
     * 补充说明 / Supplementary summary: 执行 heartbeatAndRecover 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the heartbeat and recover operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.heartbeatAndRecover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public synchronized void heartbeatAndRecover() {
        RpcGatewaySubsystemState currentState = state.get();
        if (actualPort == null
                || currentState == RpcGatewaySubsystemState.DISABLED
                || currentState == RpcGatewaySubsystemState.DRAINING
                || currentState == RpcGatewaySubsystemState.STOPPED) {
            return;
        }
        if (lease == null) {
            registerRecoverably();
            return;
        }
        if (currentState != RpcGatewaySubsystemState.REGISTERED_READY
                && currentState != RpcGatewaySubsystemState.RECOVERING) {
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
                ), true);
                return;
            }
            if (result.leaseExpireAt() == null) {
                enterRecovery(new IllegalStateException(
                        "RPC Gateway slot renewal has no lease expiry"
                ), false);
                return;
            }
            renewLeaseExpiry(result.leaseExpireAt());
            lastFailure = null;
            state.set(RpcGatewaySubsystemState.REGISTERED_READY);
        } catch (RuntimeException failure) {
            enterRecovery(failure, false);
        }
    }

    /**
     * 中文说明：执行 beginDrain 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin drain operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.beginDrain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public synchronized void beginDrain() {
        if (!properties.enabled()
                || state.get() == RpcGatewaySubsystemState.STOPPED) {
            return;
        }
        state.set(RpcGatewaySubsystemState.DRAINING);
        scheduler.shutdownNow();
        deregister();
    }

    /**
     * 中文说明：执行 state 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the state operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.state(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 state 的处理结果；returns the result of the operation.
     */
    public RpcGatewaySubsystemState state() {
        return state.get();
    }

    /**
     * 中文说明：执行 租约 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lease operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.lease(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 租约 的处理结果；returns the result of the operation.
     */
    public Optional<DdcLeaseSession> lease() {
        return Optional.ofNullable(lease);
    }

    /**
     * 中文说明：执行 lastFailure 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the last failure operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.lastFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 lastFailure 的处理结果；returns the result of the operation.
     */
    public Optional<RuntimeException> lastFailure() {
        return Optional.ofNullable(lastFailure);
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public synchronized void close() {
        beginDrain();
        state.set(RpcGatewaySubsystemState.STOPPED);
    }

    /**
     * 中文说明：执行 heartbeatSafely 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the heartbeat safely operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.heartbeatSafely(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void heartbeatSafely() {
        try {
            heartbeatAndRecover();
        } catch (RuntimeException failure) {
            synchronized (this) {
                enterRecovery(failure, false);
            }
        }
    }

    /**
     * 中文说明：执行 scheduleHeartbeat 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the schedule heartbeat operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.scheduleHeartbeat(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 registerRecoverably 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register recoverably operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.registerRecoverably(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void registerRecoverably() {
        try {
            registration = registration();
            lease = Objects.requireNonNull(
                    registry.register(registration),
                    "registered lease"
            );
            lastFailure = null;
            state.set(RpcGatewaySubsystemState.REGISTERED_READY);
        } catch (RuntimeException failure) {
            enterRecovery(failure, false);
        }
    }

    /**
     * 中文说明：执行 enterRecovery 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the enter recovery operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.enterRecovery(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @param leaseLost 参数 租约Lost；parameter lease lost。
     */
    private void enterRecovery(
            RuntimeException failure,
            boolean leaseLost) {
        RpcGatewaySubsystemState currentState = state.get();
        if (currentState == RpcGatewaySubsystemState.DRAINING
                || currentState == RpcGatewaySubsystemState.STOPPED
                || currentState == RpcGatewaySubsystemState.DISABLED) {
            return;
        }
        if (leaseLost) {
            lease = null;
        }
        lastFailure = failure;
        state.set(RpcGatewaySubsystemState.RECOVERING);
    }

    /**
     * 中文说明：执行 renew租约Expiry 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the renew lease expiry operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.renewLeaseExpiry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param leaseExpireAt 参数 租约ExpireAt；parameter lease expire at。
     */
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

    /**
     * 中文说明：执行 deregister 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deregister operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.deregister(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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
     * 补充说明 / Supplementary summary: 执行 registration 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the registration operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.registration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 补充说明 / Supplementary summary: 执行 准入Ticket 操作；该方法是 {@code RpcGatewaySlotRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the admission ticket operation; this method is the invocation entry point on {@code RpcGatewaySlotRuntime} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewaySlotRuntime.admissionTicket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
