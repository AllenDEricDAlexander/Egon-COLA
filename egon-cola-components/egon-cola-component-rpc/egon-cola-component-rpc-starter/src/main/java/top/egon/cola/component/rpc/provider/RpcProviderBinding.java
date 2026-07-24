package top.egon.cola.component.rpc.provider;

import top.egon.cola.component.rpc.contract.RpcContractDescriptor;

public record RpcProviderBinding(
        Object bean,
        RpcContractDescriptor contract
) {

    public RpcServiceIdentity serviceIdentity() {
        return RpcServiceIdentity.from(contract);
    }
}
