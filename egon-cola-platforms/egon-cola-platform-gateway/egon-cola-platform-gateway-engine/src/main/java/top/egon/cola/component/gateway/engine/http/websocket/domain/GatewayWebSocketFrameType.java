package top.egon.cola.component.gateway.engine.http.websocket.domain;

/**
 * 中文说明：{@code GatewayWebSocketFrameType} 是枚举类型，位于当前 Gateway 模块的相关包中，负责网关WebSocketFrameType相关的职责与边界。
 * English summary: {@code GatewayWebSocketFrameType} is an enumeration in the current Gateway module; it owns the gateway web socket frame type-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum GatewayWebSocketFrameType {
    /**
     * 中文说明：表示 TEXT 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value text; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    TEXT,
    /**
     * 中文说明：表示 BINARY 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value binary; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    BINARY,
    /**
     * 中文说明：表示 CONTINUATION 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value continuation; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    CONTINUATION,
    /**
     * 中文说明：表示 PING 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value ping; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    PING,
    /**
     * 中文说明：表示 PONG 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value pong; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    PONG,
    /**
     * 中文说明：表示 CLOSE 这一固定值；它属于 {@code GatewayWebSocketFrameType} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value close; it is a state, type, or protocol value of {@code GatewayWebSocketFrameType} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrameType} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrameType}; do not couple callers to its representation when the owning type exposes an API.
     */
    CLOSE
}
