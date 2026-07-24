package top.egon.cola.component.rpc.provider;

import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;

public record RpcProviderMethodBinding(
        RpcProviderBinding provider,
        RpcMethodDescriptor method
) {
}
