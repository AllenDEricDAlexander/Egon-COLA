package top.egon.cola.component.gateway.contract.error;

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
