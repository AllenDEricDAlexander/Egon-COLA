package top.egon.cola.component.gateway.engine.rpc;

public enum RpcGatewaySubsystemState {

    DISABLED,
    STARTING,
    LISTENING_NOT_REGISTERED,
    REGISTERED_READY,
    RECOVERING,
    DRAINING,
    FAILED,
    STOPPED
}
