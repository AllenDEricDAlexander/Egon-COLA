package top.egon.cola.component.gateway.engine.http.common.logging;

/**
 * 中文说明：{@code GatewayBodyLogDirection} 是枚举类型，位于当前 Gateway 模块的相关包中，负责网关BodyLogDirection相关的职责与边界。
 * English summary: {@code GatewayBodyLogDirection} is an enumeration in the current Gateway module; it owns the gateway body log direction-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum GatewayBodyLogDirection {
    /**
     * 中文说明：表示 请求 这一固定值；它属于 {@code GatewayBodyLogDirection} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request; it is a state, type, or protocol value of {@code GatewayBodyLogDirection} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBodyLogDirection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogDirection}; do not couple callers to its representation when the owning type exposes an API.
     */
    REQUEST,
    /**
     * 中文说明：表示 响应 这一固定值；它属于 {@code GatewayBodyLogDirection} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value response; it is a state, type, or protocol value of {@code GatewayBodyLogDirection} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBodyLogDirection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogDirection}; do not couple callers to its representation when the owning type exposes an API.
     */
    RESPONSE,
    /**
     * 中文说明：表示 WebSocket 这一固定值；它属于 {@code GatewayBodyLogDirection} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value websocket; it is a state, type, or protocol value of {@code GatewayBodyLogDirection} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBodyLogDirection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogDirection}; do not couple callers to its representation when the owning type exposes an API.
     */
    WEBSOCKET
}
