package top.egon.cola.component.rpc.provider.lifecycle;

/** Explicit Provider lifecycle authority used by availability and shutdown gates. */
public enum RpcProviderRuntimeState {
    NEW,
    STARTING,
    READY,
    DEGRADED,
    DRAINING,
    FAILED,
    STOPPED;

    public boolean servingNewCalls() {
        return this == READY || this == DEGRADED;
    }
}
