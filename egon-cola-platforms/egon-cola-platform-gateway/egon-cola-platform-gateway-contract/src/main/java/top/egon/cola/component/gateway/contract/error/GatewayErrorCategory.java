package top.egon.cola.component.gateway.contract.error;

/**
 * 网关错误的稳定分类，用于客户端判断失败发生在请求、治理策略还是提供方。
 */
public enum GatewayErrorCategory {

    REQUEST_INVALID,

    ROUTE_NOT_FOUND,

    EXTERNAL_NOT_ACCESSIBLE,

    AUTHENTICATION_FAILED,

    AUTHORIZATION_DENIED,

    POLICY_REJECTED,

    PROVIDER_UNAVAILABLE,

    UPSTREAM_TIMEOUT,

    UPSTREAM_FAILURE,

    RULE_NOT_READY,

    INTERNAL_ERROR
}
