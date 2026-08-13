package top.egon.cola.component.rpc.provider.metadata;

import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.util.Map;

@FunctionalInterface
public interface RpcProviderMetadataContributor {

    Map<String, String> contribute(RpcServiceIdentity serviceIdentity);
}
