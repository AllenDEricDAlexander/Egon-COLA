package top.egon.cola.component.gateway.provider;

public enum HttpProviderRuntimeState {

    NEW,
    WAITING_SERVER,
    REGISTERING,
    REGISTERED,
    RECOVERING,
    FAILED,
    STOPPED
}
