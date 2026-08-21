package top.egon.cola.component.rpc.consumer.lifecycle;

/** Complete Consumer lifecycle state, including drain and failed startup. */
public enum RpcConsumerRuntimeState {
    NEW,
    STARTING,
    READY,
    DEGRADED,
    DRAINING,
    FAILED,
    STOPPED;

    public boolean accepting() {
        return this == READY || this == DEGRADED;
    }
}
