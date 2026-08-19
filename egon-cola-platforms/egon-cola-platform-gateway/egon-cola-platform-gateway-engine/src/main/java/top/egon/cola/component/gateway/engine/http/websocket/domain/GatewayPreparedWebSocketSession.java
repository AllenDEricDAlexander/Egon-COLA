package top.egon.cola.component.gateway.engine.http.websocket.domain;

import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An already accepted upstream session held before the downstream 101.
 * 补充说明 / Supplementary summary: {@code GatewayPreparedWebSocketSession} 是类型，位于当前 Gateway 模块的相关包中，负责网关PreparedWebSocket会话相关的职责与边界。
 * English supplement: {@code GatewayPreparedWebSocketSession} is a type in the current Gateway module; it owns the gateway prepared web socket session-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayPreparedWebSocketSession implements AutoCloseable {

    /**
     * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketProxyContext}，由 {@code GatewayPreparedWebSocketSession} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayWebSocketProxyContext}, and {@code GatewayPreparedWebSocketSession} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPreparedWebSocketSession} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPreparedWebSocketSession}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayWebSocketProxyContext context;

    /**
     * 中文说明：保存 upstream 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketPeer}，由 {@code GatewayPreparedWebSocketSession} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by upstream; its type is {@code GatewayWebSocketPeer}, and {@code GatewayPreparedWebSocketSession} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPreparedWebSocketSession} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPreparedWebSocketSession}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayWebSocketPeer upstream;

    /**
     * 中文说明：保存 selectedSubprotocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayPreparedWebSocketSession} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by selected subprotocol; its type is {@code String}, and {@code GatewayPreparedWebSocketSession} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPreparedWebSocketSession} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPreparedWebSocketSession}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String selectedSubprotocol;

    /**
     * 中文说明：保存 disposeAction 对应的状态、依赖或配置值；字段类型为 {@code Runnable}，由 {@code GatewayPreparedWebSocketSession} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dispose action; its type is {@code Runnable}, and {@code GatewayPreparedWebSocketSession} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPreparedWebSocketSession} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPreparedWebSocketSession}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Runnable disposeAction;

    /**
     * 中文说明：保存 disposed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayPreparedWebSocketSession} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by disposed; its type is {@code AtomicBoolean}, and {@code GatewayPreparedWebSocketSession} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPreparedWebSocketSession} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPreparedWebSocketSession}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();

    /**
     * 中文说明：创建 {@code GatewayPreparedWebSocketSession} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayPreparedWebSocketSession} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param context 参数 context；parameter context。
     * @param upstream 参数 upstream；parameter upstream。
     * @param selectedSubprotocol 参数 selectedSubprotocol；parameter selected subprotocol。
     */
    public GatewayPreparedWebSocketSession(
            GatewayWebSocketProxyContext context,
            GatewayWebSocketPeer upstream,
            String selectedSubprotocol) {
        this(context, upstream, selectedSubprotocol, () -> {
        });
    }

    /**
     * 中文说明：创建 {@code GatewayPreparedWebSocketSession} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayPreparedWebSocketSession} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param context 参数 context；parameter context。
     * @param upstream 参数 upstream；parameter upstream。
     * @param selectedSubprotocol 参数 selectedSubprotocol；parameter selected subprotocol。
     * @param disposeAction 参数 disposeAction；parameter dispose action。
     */
    private GatewayPreparedWebSocketSession(
            GatewayWebSocketProxyContext context,
            GatewayWebSocketPeer upstream,
            String selectedSubprotocol,
            Runnable disposeAction) {
        this.context = Objects.requireNonNull(context, "context");
        this.upstream = Objects.requireNonNull(upstream, "upstream");
        this.selectedSubprotocol = selectedSubprotocol == null
                || selectedSubprotocol.isBlank()
                ? null
                : selectedSubprotocol;
        this.disposeAction = Objects.requireNonNull(
                disposeAction,
                "disposeAction"
        );
    }

    /**
     * 中文说明：执行 context 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the context operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.context(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 context 的处理结果；returns the result of the operation.
     */
    public GatewayWebSocketProxyContext context() {
        return context;
    }

    /**
     * 中文说明：执行 upstream 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upstream operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.upstream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 upstream 的处理结果；returns the result of the operation.
     */
    public GatewayWebSocketPeer upstream() {
        return upstream;
    }

    /**
     * 中文说明：执行 selectedSubprotocol 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the selected subprotocol operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.selectedSubprotocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 selectedSubprotocol 的处理结果；returns the result of the operation.
     */
    public String selectedSubprotocol() {
        return selectedSubprotocol;
    }

    /**
     * 中文说明：执行 dispose 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispose operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.dispose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 dispose 的处理结果；returns the result of the operation.
     */
    public boolean dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return false;
        }
        try {
            upstream.dispose();
        } finally {
            disposeAction.run();
        }
        return true;
    }

    /**
     * 中文说明：执行 onDispose 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on dispose operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.onDispose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param action 参数 action；parameter action。
     * @return 返回 onDispose 的处理结果；returns the result of the operation.
     */
    public GatewayPreparedWebSocketSession onDispose(Runnable action) {
        Objects.requireNonNull(action, "action");
        return new GatewayPreparedWebSocketSession(
                context,
                upstream,
                selectedSubprotocol,
                () -> {
                    try {
                        dispose();
                    } finally {
                        action.run();
                    }
                }
        );
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayPreparedWebSocketSession} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayPreparedWebSocketSession} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPreparedWebSocketSession.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        dispose();
    }
}
