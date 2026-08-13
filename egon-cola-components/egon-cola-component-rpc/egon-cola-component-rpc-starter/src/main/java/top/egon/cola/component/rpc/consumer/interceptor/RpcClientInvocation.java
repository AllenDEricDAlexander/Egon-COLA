package top.egon.cola.component.rpc.consumer.interceptor;

import com.google.protobuf.Message;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;

/**
 * 创建请求感知客户端拦截器时可见的不可变调用上下文。
 *
 * <p>Immutable invocation context available when creating a request-aware
 * client interceptor.
 *
 * @param contract RPC 契约 / RPC contract
 * @param method RPC 方法 / RPC method
 * @param request Protobuf 请求 / Protobuf request
 * @param processIdentity 调用方进程身份 / caller process identity
 */
public record RpcClientInvocation(
        RpcContractDescriptor contract,
        RpcMethodDescriptor method,
        Message request,
        RpcProcessIdentity processIdentity
) {

    public RpcClientInvocation {
        if (contract == null || method == null || request == null
                || processIdentity == null) {
            throw new IllegalArgumentException(
                    "RPC client invocation fields are required"
            );
        }
    }
}
