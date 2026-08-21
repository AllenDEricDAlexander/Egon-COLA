package top.egon.cola.component.rpc.provider.binding;

import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

public record RpcProviderBinding(
        Object bean,
        RpcContractDescriptor contract
) {

    public RpcProviderBinding {
        if (bean == null || contract == null) {
            throw new IllegalArgumentException("RPC provider binding requires bean and contract");
        }
    }

    public RpcServiceIdentity serviceIdentity() {
        return RpcServiceIdentity.from(contract);
    }
}
