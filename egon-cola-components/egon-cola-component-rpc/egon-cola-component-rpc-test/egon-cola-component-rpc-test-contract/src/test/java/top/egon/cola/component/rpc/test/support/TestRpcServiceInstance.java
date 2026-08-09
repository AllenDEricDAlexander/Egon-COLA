package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.time.Instant;

public record TestRpcServiceInstance(
        RpcServiceIdentity serviceIdentity,
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Instant leaseExpireAt,
        long revision
) implements Comparable<TestRpcServiceInstance> {

    static TestRpcServiceInstance from(
            RpcProviderRegistration registration,
            RpcProviderLease lease,
            long revision) {
        return new TestRpcServiceInstance(
                registration.serviceIdentity(),
                lease.instanceId(),
                lease.leaseId(),
                registration.host(),
                registration.port(),
                registration.secure(),
                lease.leaseExpireAt(),
                revision
        );
    }

    @Override
    public int compareTo(TestRpcServiceInstance other) {
        int serviceOrder = serviceIdentity.registrySuffix().compareTo(
                other.serviceIdentity.registrySuffix()
        );
        if (serviceOrder != 0) {
            return serviceOrder;
        }
        return instanceId.compareTo(other.instanceId);
    }
}
