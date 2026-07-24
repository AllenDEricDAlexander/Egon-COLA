package top.egon.cola.component.rpc.provider;

import top.egon.cola.component.rpc.contract.RpcContractDescriptor;

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
