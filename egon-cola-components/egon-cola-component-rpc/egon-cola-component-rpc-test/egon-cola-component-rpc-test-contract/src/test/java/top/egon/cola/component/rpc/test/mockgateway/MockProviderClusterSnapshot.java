package top.egon.cola.component.rpc.test.mockgateway;

import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.util.List;

record MockProviderClusterSnapshot(
        RpcServiceIdentity serviceIdentity,
        long revision,
        List<MockProviderEndpoint> endpoints
) {

    MockProviderClusterSnapshot {
        endpoints = endpoints.stream().sorted().toList();
    }
}
