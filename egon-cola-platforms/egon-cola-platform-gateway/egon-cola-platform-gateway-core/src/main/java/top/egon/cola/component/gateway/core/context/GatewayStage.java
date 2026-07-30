package top.egon.cola.component.gateway.core.context;

public enum GatewayStage {

    RECEIVED,

    ROUTE_MATCHED,

    SECURITY_CHECKED,

    GOVERNED,

    PROVIDER_SELECTED,

    UPSTREAM_INVOKED,

    COMPLETED
}
