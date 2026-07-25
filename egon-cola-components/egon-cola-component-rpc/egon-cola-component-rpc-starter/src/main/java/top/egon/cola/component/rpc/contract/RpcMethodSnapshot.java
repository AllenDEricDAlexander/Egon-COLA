package top.egon.cola.component.rpc.contract;

import java.util.Objects;

public record RpcMethodSnapshot(
        String methodName,
        String fullMethodName,
        String requestType,
        String responseType,
        RpcType rpcType
) {

    public RpcMethodSnapshot {
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(fullMethodName, "fullMethodName");
        Objects.requireNonNull(requestType, "requestType");
        Objects.requireNonNull(responseType, "responseType");
        Objects.requireNonNull(rpcType, "rpcType");
    }
}
