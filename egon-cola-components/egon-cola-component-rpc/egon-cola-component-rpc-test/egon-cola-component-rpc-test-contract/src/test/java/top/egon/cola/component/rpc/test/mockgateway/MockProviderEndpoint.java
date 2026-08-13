package top.egon.cola.component.rpc.test.mockgateway;

import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.support.TestRpcServiceInstance;

import java.time.Instant;

record MockProviderEndpoint(
        RpcServiceIdentity serviceIdentity,
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Instant leaseExpireAt
) implements Comparable<MockProviderEndpoint> {

    static MockProviderEndpoint from(TestRpcServiceInstance instance) {
        return new MockProviderEndpoint(
                instance.serviceIdentity(),
                instance.instanceId(),
                instance.leaseId(),
                instance.host(),
                instance.port(),
                instance.secure(),
                instance.leaseExpireAt()
        );
    }

    boolean activeAt(Instant now) {
        return leaseExpireAt != null && leaseExpireAt.isAfter(now);
    }

    String channelKey() {
        return String.join(
                ":",
                instanceId,
                leaseId,
                host,
                Integer.toString(port),
                Boolean.toString(secure)
        );
    }

    @Override
    public int compareTo(MockProviderEndpoint other) {
        int instanceOrder = instanceId.compareTo(other.instanceId);
        return instanceOrder == 0
                ? leaseId.compareTo(other.leaseId)
                : instanceOrder;
    }
}
