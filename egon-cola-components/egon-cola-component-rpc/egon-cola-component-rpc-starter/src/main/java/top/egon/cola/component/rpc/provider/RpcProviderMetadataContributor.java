package top.egon.cola.component.rpc.provider;

import java.util.Map;

@FunctionalInterface
public interface RpcProviderMetadataContributor {

    Map<String, String> contribute(RpcServiceIdentity serviceIdentity);
}
