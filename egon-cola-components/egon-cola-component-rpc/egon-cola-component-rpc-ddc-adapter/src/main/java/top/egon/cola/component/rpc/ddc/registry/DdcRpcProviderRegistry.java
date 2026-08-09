package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.rpc.provider.*;

import java.util.Map;

/** RPC Provider 中立注册 SPI 到 DDC 注册 Port 的桥接。 / Bridge from neutral RPC Provider registration to DDC. */
public final class DdcRpcProviderRegistry implements RpcProviderRegistry {

    private final DdcServiceRegistryClient client;
    private final String bizCode;
    private final String appCode;

    public DdcRpcProviderRegistry(DdcServiceRegistryClient client, String bizCode, String appCode) {
        this.client = client;
        this.bizCode = bizCode;
        this.appCode = appCode;
    }

    @Override
    public RpcProviderLease register(RpcProviderRegistration registration) {
        validateMetadata(registration.metadata());
        var session = client.register(new DdcServiceRegistration(
                registration.processIdentity().instanceId(),
                key(registration),
                registration.host(), registration.port(), registration.secure(),
                registration.metadata(), registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds()
        ));
        return new RpcProviderLease(
                session.instanceId(), session.leaseId(),
                session.registeredAt(), session.leaseExpireAt()
        );
    }

    @Override
    public RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease) {
        return operation(client.heartbeat(lease.instanceId(), lease.leaseId()));
    }

    @Override
    public RpcLeaseOperationResult deregister(RpcProviderLeaseIdentity lease) {
        return operation(client.deregister(lease.instanceId(), lease.leaseId()));
    }

    private DdcServiceKey key(RpcProviderRegistration registration) {
        RpcServiceIdentity service = registration.serviceIdentity();
        return new DdcServiceKey(
                bizCode, registration.processIdentity().env(), appCode,
                DdcServiceKind.RPC_PROVIDER, service.serviceName(),
                service.group(), service.version(), "grpc"
        );
    }

    private RpcLeaseOperationResult operation(DdcLeaseOperationResult result) {
        return switch (result.status()) {
            case RENEWED -> RpcLeaseOperationResult.renewed(result.leaseExpireAt());
            case DELETED -> RpcLeaseOperationResult.deleted();
            case NOT_FOUND -> RpcLeaseOperationResult.notFound();
            case LEASE_MISMATCH -> RpcLeaseOperationResult.leaseMismatch();
            default -> RpcLeaseOperationResult.notFound();
        };
    }

    private void validateMetadata(Map<String, String> metadata) {
        metadata.forEach((key, value) -> {
            if (ServiceInstanceMetaCodec.isReservedKey(key)) {
                ServiceInstanceMetaCodec.validate(key, value);
            }
        });
    }
}
