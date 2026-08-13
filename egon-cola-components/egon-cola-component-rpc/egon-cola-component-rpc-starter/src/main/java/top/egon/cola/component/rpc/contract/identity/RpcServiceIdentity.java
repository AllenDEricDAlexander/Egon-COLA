package top.egon.cola.component.rpc.contract.identity;

import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;

public record RpcServiceIdentity(
        String serviceName,
        String group,
        String version
) {

    public static RpcServiceIdentity from(RpcContractDescriptor descriptor) {
        return new RpcServiceIdentity(
                descriptor.serviceName(),
                descriptor.group(),
                descriptor.version()
        );
    }

    public String registrySuffix() {
        return String.join(":", serviceName, group, version);
    }
}
