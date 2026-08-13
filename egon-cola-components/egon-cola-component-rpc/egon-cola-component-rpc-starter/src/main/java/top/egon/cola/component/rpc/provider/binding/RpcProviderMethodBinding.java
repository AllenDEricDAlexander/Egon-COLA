package top.egon.cola.component.rpc.provider.binding;

import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;

public record RpcProviderMethodBinding(
        RpcProviderBinding provider,
        RpcMethodDescriptor method
) {
}
