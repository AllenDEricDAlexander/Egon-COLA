package top.egon.cola.component.gateway.engine.websocket;

import java.util.Objects;

/**
 * 中文说明：{@code GatewayWebSocketHandshakeResult} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关WebSocketHandshakeResult相关的职责与边界。
 * English summary: {@code GatewayWebSocketHandshakeResult} is an interface contract in the current Gateway module; it owns the gateway web socket handshake result-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public sealed interface GatewayWebSocketHandshakeResult
        permits GatewayWebSocketHandshakeResult.Accepted,
        GatewayWebSocketHandshakeResult.Rejected {

    /**
     * 中文说明：执行 accepted 操作；该方法是 {@code GatewayWebSocketHandshakeResult} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the accepted operation; this method is the invocation entry point on {@code GatewayWebSocketHandshakeResult} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketHandshakeResult.accepted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param session 参数 会话；parameter session。
     * @return 返回 accepted 的处理结果；returns the result of the operation.
     */
    static Accepted accepted(GatewayPreparedWebSocketSession session) {
        return new Accepted(session);
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code GatewayWebSocketHandshakeResult} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code GatewayWebSocketHandshakeResult} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketHandshakeResult.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    public static Rejected rejected(
            int httpStatus,
            String errorCode,
            String message) {
        return new Rejected(httpStatus, errorCode, message);
    }

    /**
     * 中文说明：{@code Accepted} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Accepted相关的职责与边界。
     * English summary: {@code Accepted} is an immutable data carrier in the current Gateway module; it owns the accepted-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param session 参数 会话；parameter session。
     */
    record Accepted(
    /**
     * 中文说明：保存 会话 对应的状态、依赖或配置值；字段类型为 {@code GatewayPreparedWebSocketSession}，由 {@code GatewayWebSocketHandshakeResult.Accepted} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by session; its type is {@code GatewayPreparedWebSocketSession}, and {@code GatewayWebSocketHandshakeResult.Accepted} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketHandshakeResult.Accepted} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketHandshakeResult.Accepted}; do not couple callers to its representation when the owning type exposes an API.
     */
    GatewayPreparedWebSocketSession session)
            implements GatewayWebSocketHandshakeResult {

        /**
         * 中文说明：创建 {@code GatewayWebSocketHandshakeResult.Accepted} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayWebSocketHandshakeResult.Accepted} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param session 参数 会话；parameter session。
         */
        public Accepted {
            session = Objects.requireNonNull(session, "session");
        }
    }

    /**
     * 中文说明：{@code Rejected} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rejected相关的职责与边界。
     * English summary: {@code Rejected} is an immutable data carrier in the current Gateway module; it owns the rejected-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param message 参数 消息；parameter message。
     */
    record Rejected(
            /**
             * 中文说明：保存 httpStatus 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayWebSocketHandshakeResult.Rejected} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by http status; its type is {@code int}, and {@code GatewayWebSocketHandshakeResult.Rejected} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayWebSocketHandshakeResult.Rejected} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketHandshakeResult.Rejected}; do not couple callers to its representation when the owning type exposes an API.
             */
            int httpStatus,
            /**
             * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayWebSocketHandshakeResult.Rejected} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayWebSocketHandshakeResult.Rejected} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayWebSocketHandshakeResult.Rejected} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketHandshakeResult.Rejected}; do not couple callers to its representation when the owning type exposes an API.
             */
            String errorCode,
            /**
             * 中文说明：保存 消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayWebSocketHandshakeResult.Rejected} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by message; its type is {@code String}, and {@code GatewayWebSocketHandshakeResult.Rejected} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayWebSocketHandshakeResult.Rejected} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketHandshakeResult.Rejected}; do not couple callers to its representation when the owning type exposes an API.
             */
            String message
    ) implements GatewayWebSocketHandshakeResult {

        /**
         * 中文说明：创建 {@code GatewayWebSocketHandshakeResult.Rejected} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayWebSocketHandshakeResult.Rejected} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param httpStatus 参数 httpStatus；parameter http status。
         * @param errorCode 参数 errorCode；parameter error code。
         * @param message 参数 消息；parameter message。
         */
        public Rejected {
            if (httpStatus < 400 || httpStatus > 599) {
                throw new IllegalArgumentException(
                        "rejected handshake requires 4xx or 5xx status"
                );
            }
            if (errorCode == null || errorCode.isBlank()) {
                throw new IllegalArgumentException(
                        "errorCode is required"
                );
            }
            message = message == null ? "" : message;
        }
    }
}
