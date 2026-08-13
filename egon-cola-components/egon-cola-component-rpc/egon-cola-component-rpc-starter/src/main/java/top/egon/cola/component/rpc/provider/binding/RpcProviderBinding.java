package top.egon.cola.component.rpc.provider.binding;

import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

public record RpcProviderBinding(
        Object bean,
        RpcContractDescriptor contract
) {

    public RpcServiceIdentity serviceIdentity() {
        return RpcServiceIdentity.from(contract);
    }
}
