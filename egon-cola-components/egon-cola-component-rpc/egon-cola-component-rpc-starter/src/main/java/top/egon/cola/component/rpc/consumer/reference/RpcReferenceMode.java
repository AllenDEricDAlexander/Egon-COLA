package top.egon.cola.component.rpc.consumer.reference;

/**
 * Client-selected transport mode; there is intentionally no AUTO value.
 * {@link #DIRECT} is the default for {@code @EgonRpcReference}; Gateway proxy
 * calls must select {@link #GATEWAY} explicitly.
 */
public enum RpcReferenceMode {
    GATEWAY,
    DIRECT
}
