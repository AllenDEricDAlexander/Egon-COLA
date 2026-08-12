package top.egon.cola.component.gateway.engine;

import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySubsystemState;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：{@code GatewayEngineRuntime} 是运行时组件，位于当前 Gateway 模块的相关包中，负责网关引擎运行时相关的职责与边界。
 * English summary: {@code GatewayEngineRuntime} is a gateway engine runtime runtime in the current Gateway module; it owns the gateway engine runtime-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayEngineRuntime implements SmartLifecycle {

    /**
     * 中文说明：保存 properties 对应的状态、依赖或配置值；字段类型为 {@code GatewayEngineRuntimeProperties}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by properties; its type is {@code GatewayEngineRuntimeProperties}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayEngineRuntimeProperties properties;

    /**
     * 中文说明：保存 http服务器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpServer}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http server; its type is {@code GatewayHttpServer}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpServer httpServer;

    /**
     * 中文说明：保存 rpc服务器 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewayServer}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc server; its type is {@code RpcGatewayServer}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewayServer rpcServer;

    /**
     * 中文说明：保存 rpc槽位 对应的状态、依赖或配置值；字段类型为 {@code RpcGatewaySlotRuntime}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc slot; its type is {@code RpcGatewaySlotRuntime}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcGatewaySlotRuntime rpcSlot;

    /**
     * 中文说明：保存 activation 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleActivationApplier}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by activation; its type is {@code GatewayRuleActivationApplier}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleActivationApplier activation;

    /**
     * 中文说明：保存 提供方Directory 对应的状态、依赖或配置值；字段类型为 {@code ProviderDirectory}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider directory; its type is {@code ProviderDirectory}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderDirectory providerDirectory;

    /**
     * 中文说明：保存 running 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by running; its type is {@code boolean}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile boolean running;

    /**
     * 中文说明：保存 ready 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ready; its type is {@code boolean}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile boolean ready;

    /**
     * 中文说明：保存 coordinator 对应的状态、依赖或配置值；字段类型为 {@code ScheduledExecutorService}，由 {@code GatewayEngineRuntime} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by coordinator; its type is {@code ScheduledExecutorService}, and {@code GatewayEngineRuntime} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntime} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntime}; do not couple callers to its representation when the owning type exposes an API.
     */
    private ScheduledExecutorService coordinator;

    /**
     * 中文说明：创建 {@code GatewayEngineRuntime} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayEngineRuntime} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param properties 参数 properties；parameter properties。
     * @param httpServer 参数 http服务器；parameter http server。
     * @param rpcServer 参数 rpc服务器；parameter rpc server。
     * @param rpcSlot 参数 rpc槽位；parameter rpc slot。
     * @param activation 参数 activation；parameter activation。
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     */
    public GatewayEngineRuntime(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer,
            RpcGatewaySlotRuntime rpcSlot,
            GatewayRuleActivationApplier activation,
            ProviderDirectory providerDirectory) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.httpServer = Objects.requireNonNull(httpServer, "httpServer");
        this.rpcServer = Objects.requireNonNull(rpcServer, "rpcServer");
        this.rpcSlot = Objects.requireNonNull(rpcSlot, "rpcSlot");
        this.activation = Objects.requireNonNull(activation, "activation");
        this.providerDirectory = Objects.requireNonNull(
                providerDirectory,
                "providerDirectory"
        );
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        restoreRulesSafely();
        httpServer.start();
        if (properties.getRpc().isEnabled()) {
            rpcServer.start();
            rpcSlot.listenerStarted(rpcServer.port());
        }
        running = true;
        coordinator = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "gateway-engine-readiness"
            );
            thread.setDaemon(true);
            return thread;
        });
        refreshReadiness();
        coordinator.scheduleWithFixedDelay(
                this::refreshReadinessSafely,
                250,
                250,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 中文说明：执行 stop 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stop operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.stop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public synchronized void stop() {
        ready = false;
        running = false;
        if (coordinator != null) {
            coordinator.shutdownNow();
            coordinator = null;
        }
        httpServer.beginDrain();
        rpcSlot.beginDrain();
        httpServer.awaitDrain();
        rpcServer.close();
        httpServer.close();
    }

    /**
     * 中文说明：执行 isRunning 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is running operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.isRunning(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isRunning 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 中文说明：执行 running 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the running operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.running(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 running 的处理结果；returns the result of the operation.
     */
    public boolean running() {
        return running;
    }

    /**
     * 中文说明：执行 ready 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ready operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.ready(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 ready 的处理结果；returns the result of the operation.
     */
    public boolean ready() {
        return ready;
    }

    /**
     * 中文说明：执行 rpcState 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc state operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.rpcState(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 rpcState 的处理结果；returns the result of the operation.
     */
    public RpcGatewaySubsystemState rpcState() {
        return rpcSlot.state();
    }

    /**
     * 中文说明：执行 getPhase 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get phase operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.getPhase(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getPhase 的处理结果；returns the result of the operation.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 中文说明：执行 refreshReadiness 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the refresh readiness operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.refreshReadiness(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private synchronized void refreshReadiness() {
        boolean rulesReady = restoreRulesSafely()
                && activation.status().ready();
        boolean providersReady = rulesReady
                && providerDirectory.allAvailable(
                activation.active().providerServices()
        );
        if (rulesReady
                && providersReady
                && properties.getRpc().isEnabled()
                && rpcSlot.state()
                == RpcGatewaySubsystemState.LISTENING_NOT_REGISTERED) {
            rpcSlot.engineReady();
        }
        boolean rpcReady = !properties.getRpc().isEnabled()
                || rpcSlot.state()
                == RpcGatewaySubsystemState.REGISTERED_READY;
        ready = running
                && httpServer.accepting()
                && rulesReady
                && providersReady
                && rpcReady;
    }

    /**
     * 中文说明：执行 refreshReadinessSafely 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the refresh readiness safely operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.refreshReadinessSafely(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void refreshReadinessSafely() {
        try {
            refreshReadiness();
        } catch (RuntimeException failure) {
            ready = false;
        }
    }

    /**
     * 中文说明：执行 restoreRulesSafely 操作；该方法是 {@code GatewayEngineRuntime} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the restore rules safely operation; this method is the invocation entry point on {@code GatewayEngineRuntime} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntime.restoreRulesSafely(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 restoreRulesSafely 的处理结果；returns the result of the operation.
     */
    private boolean restoreRulesSafely() {
        if (activation.active() != null) {
            return true;
        }
        try {
            activation.restoreLkg();
        } catch (RuntimeException failure) {
            ready = false;
            return false;
        }
        return activation.active() != null;
    }
}
