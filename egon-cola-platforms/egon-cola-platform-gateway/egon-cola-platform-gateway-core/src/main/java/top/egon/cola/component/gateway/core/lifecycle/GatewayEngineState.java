package top.egon.cola.component.gateway.core.lifecycle;

public enum GatewayEngineState {

    NEW,

    STARTING,

    SYNCING_RULES,

    READY,

    DEGRADED,

    DRAINING,

    STOPPED,

    FAILED
}
