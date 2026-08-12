package top.egon.cola.component.gateway.engine.transport;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Request-local monotonic state machine for HTTP or WebSocket commit facts.
 * 补充说明 / Supplementary summary: {@code GatewayCommitGuard} 是类型，位于当前 Gateway 模块的相关包中，负责网关CommitGuard相关的职责与边界。
 * English supplement: {@code GatewayCommitGuard} is a type in the current Gateway module; it owns the gateway commit guard-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCommitGuard {

    /**
     * 中文说明：保存 flow 对应的状态、依赖或配置值；字段类型为 {@code GatewayCommitPoint.Flow}，由 {@code GatewayCommitGuard} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by flow; its type is {@code GatewayCommitPoint.Flow}, and {@code GatewayCommitGuard} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitGuard} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitGuard}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCommitPoint.Flow flow;

    /**
     * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<State>}，由 {@code GatewayCommitGuard} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code AtomicReference<State>}, and {@code GatewayCommitGuard} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitGuard} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitGuard}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<State> state =
            new AtomicReference<>(new State(GatewayCommitPoint.NEW, 0));

    /**
     * 中文说明：创建 {@code GatewayCommitGuard} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCommitGuard} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param flow 参数 flow；parameter flow。
     */
    private GatewayCommitGuard(GatewayCommitPoint.Flow flow) {
        this.flow = flow;
    }

    /**
     * 中文说明：执行 http 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.http(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 http 的处理结果；returns the result of the operation.
     */
    public static GatewayCommitGuard http() {
        return new GatewayCommitGuard(GatewayCommitPoint.Flow.HTTP);
    }

    /**
     * 中文说明：执行 WebSocket 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the websocket operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.websocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 WebSocket 的处理结果；returns the result of the operation.
     */
    public static GatewayCommitGuard websocket() {
        return new GatewayCommitGuard(
                GatewayCommitPoint.Flow.WEBSOCKET
        );
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    public GatewayCommitPoint current() {
        return state.get().point();
    }

    /**
     * 中文说明：执行 advance 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the advance operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.advance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param next 参数 next；parameter next。
     * @return 返回 advance 的处理结果；returns the result of the operation.
     */
    public boolean advance(GatewayCommitPoint next) {
        Objects.requireNonNull(next, "next");
        if (!next.supports(flow)) {
            throw new IllegalArgumentException(
                    next + " is not valid for " + flow
            );
        }
        while (true) {
            State existing = state.get();
            if (existing.point() == GatewayCommitPoint.TERMINATED
                    || next != GatewayCommitPoint.TERMINATED
                    && next.rank() <= existing.rank()) {
                return false;
            }
            State updated = next == GatewayCommitPoint.TERMINATED
                    ? new State(next, existing.rank())
                    : new State(next, next.rank());
            if (state.compareAndSet(existing, updated)) {
                return true;
            }
        }
    }

    /**
     * 中文说明：执行 terminate 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the terminate operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.terminate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 terminate 的处理结果；returns the result of the operation.
     */
    public boolean terminate() {
        return advance(GatewayCommitPoint.TERMINATED);
    }

    /**
     * 中文说明：执行 upstreamAccepted 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upstream accepted operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.upstreamAccepted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 upstreamAccepted 的处理结果；returns the result of the operation.
     */
    public boolean upstreamAccepted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED
                : GatewayCommitPoint.UPSTREAM_HANDSHAKE_RECEIVED);
    }

    /**
     * 中文说明：执行 downstreamCommitted 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the downstream committed operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.downstreamCommitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 downstreamCommitted 的处理结果；returns the result of the operation.
     */
    public boolean downstreamCommitted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.DOWNSTREAM_HEADERS_COMMITTED
                : GatewayCommitPoint.CLIENT_HANDSHAKE_COMMITTED);
    }

    /**
     * 中文说明：执行 payloadCommitted 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload committed operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.payloadCommitted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 payloadCommitted 的处理结果；returns the result of the operation.
     */
    public boolean payloadCommitted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.FIRST_BODY_BUFFER_SENT
                : GatewayCommitPoint.FIRST_FRAME_FORWARDED);
    }

    /**
     * 中文说明：执行 terminated 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the terminated operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.terminated(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 terminated 的处理结果；returns the result of the operation.
     */
    public boolean terminated() {
        return state.get().point() == GatewayCommitPoint.TERMINATED;
    }

    /**
     * 中文说明：执行 hasReached 操作；该方法是 {@code GatewayCommitGuard} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the has reached operation; this method is the invocation entry point on {@code GatewayCommitGuard} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCommitGuard.hasReached(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param point 参数 point；parameter point。
     * @return 返回 hasReached 的处理结果；returns the result of the operation.
     */
    private boolean hasReached(GatewayCommitPoint point) {
        return state.get().rank() >= point.rank();
    }

    /**
     * 中文说明：{@code State} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责State相关的职责与边界。
     * English summary: {@code State} is an immutable data carrier in the current Gateway module; it owns the state-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param point 参数 point；parameter point。
     * @param rank 参数 rank；parameter rank。
     */
    private record State(
    /**
     * 中文说明：保存 point 对应的状态、依赖或配置值；字段类型为 {@code GatewayCommitPoint}，由 {@code GatewayCommitGuard.State} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by point; its type is {@code GatewayCommitPoint}, and {@code GatewayCommitGuard.State} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitGuard.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitGuard.State}; do not couple callers to its representation when the owning type exposes an API.
     */
    GatewayCommitPoint point,
    /**
     * 中文说明：保存 rank 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCommitGuard.State} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rank; its type is {@code int}, and {@code GatewayCommitGuard.State} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCommitGuard.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCommitGuard.State}; do not couple callers to its representation when the owning type exposes an API.
     */
    int rank) {
    }
}
