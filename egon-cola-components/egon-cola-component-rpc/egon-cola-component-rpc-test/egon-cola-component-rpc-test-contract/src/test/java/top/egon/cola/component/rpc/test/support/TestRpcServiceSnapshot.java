package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.time.Instant;
import java.util.List;

public record TestRpcServiceSnapshot(
        RpcServiceIdentity serviceIdentity,
        long revision,
        List<TestRpcServiceInstance> instances,
        Instant observedAt
) {

    public TestRpcServiceSnapshot {
        instances = List.copyOf(instances);
    }
}
