package top.egon.cola.component.rpc.test.mockgateway;

import top.egon.cola.component.ddc.registry.model.DdcServiceKey;

import java.util.List;

record MockProviderClusterSnapshot(
        DdcServiceKey serviceKey,
        long revision,
        List<MockProviderEndpoint> endpoints
) {

    MockProviderClusterSnapshot {
        endpoints = endpoints.stream().sorted().toList();
    }
}
