package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.rpc.provider.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** RPC Provider 中立注册 SPI 到 DDC 注册 Port 的桥接。 / Bridge from neutral RPC Provider registration to DDC. */
public final class DdcRpcProviderRegistry implements RpcProviderRegistry {

    private final DdcServiceRegistryClient client;
    private final String bizCode;
    private final String appCode;
    /** RPC Provider 注册和心跳所需的 IdP 短期票据。 / IdP short-lived tickets required by RPC Provider registration and heartbeats. */
    private final DdcAdmissionTicketSupplier admissionTickets;

    /** 按活动租约保存服务键，不保存原始票据。 / Holds service keys by active lease and never stores raw tickets. */
    private final ConcurrentMap<String, DdcServiceKey> activeServices =
            new ConcurrentHashMap<>();

    /**
     * 创建 RPC Provider 到 DDC 的准入注册桥接器。
     * / Creates the admitted registration bridge from RPC Provider to DDC.
     *
     * @param client DDC 服务注册客户端 / DDC service-registry client
     * @param bizCode 业务域编码 / business-domain code
     * @param appCode 应用编码 / application code
     * @param admissionTickets 准入票据端口 / admission-ticket port
     */
    public DdcRpcProviderRegistry(
            DdcServiceRegistryClient client,
            String bizCode,
            String appCode,
            DdcAdmissionTicketSupplier admissionTickets) {
        this.client = Objects.requireNonNull(client, "client");
        this.bizCode = bizCode;
        this.appCode = appCode;
        this.admissionTickets = Objects.requireNonNull(
                admissionTickets,
                "admissionTickets"
        );
    }

    /**
     * 使用精确物理身份和新鲜票据注册 RPC Provider。
     * / Registers an RPC Provider with its exact physical identity and a fresh ticket.
     */
    @Override
    public RpcProviderLease register(RpcProviderRegistration registration) {
        validateMetadata(registration.metadata());
        DdcServiceKey serviceKey = key(registration);
        var session = client.register(new DdcServiceRegistration(
                registration.processIdentity().instanceId(),
                serviceKey,
                registration.host(), registration.port(), registration.secure(),
                registration.metadata(), registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                admissionTicket(serviceKey, registration.processIdentity().instanceId())
        ));
        activeServices.put(session.leaseId(), serviceKey);
        return new RpcProviderLease(
                session.instanceId(), session.leaseId(),
                session.registeredAt(), session.leaseExpireAt()
        );
    }

    /**
     * 为活动 Provider 租约携带新鲜票据续约。
     * / Renews an active Provider lease with a fresh ticket.
     */
    @Override
    public RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease) {
        DdcServiceKey serviceKey = requireActiveService(lease);
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(serviceKey);
        request.setInstanceId(lease.instanceId());
        request.setLeaseId(lease.leaseId());
        request.setAdmissionTicket(admissionTicket(
                serviceKey,
                lease.instanceId()
        ));
        DdcLeaseOperationResult result = client.heartbeat(request);
        if (!result.renewed()) {
            activeServices.remove(lease.leaseId());
        }
        return operation(result);
    }

    /**
     * 仅凭租约身份注销，不要求重新取得准入票据。
     * / Deregisters with lease identity alone and does not acquire another admission ticket.
     */
    @Override
    public RpcLeaseOperationResult deregister(RpcProviderLeaseIdentity lease) {
        DdcLeaseOperationResult result = client.deregister(
                lease.instanceId(),
                lease.leaseId()
        );
        if (result.status() != DdcLeaseOperationStatus.NOT_DELETED) {
            activeServices.remove(lease.leaseId());
        }
        return operation(result);
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

    /**
     * 为实际服务键和实例取得原始准入票据。
     * / Obtains the raw admission ticket for the actual service key and instance.
     *
     * @param serviceKey 实际发送的服务键 / service key actually sent
     * @param instanceId 实际发送的实例标识 / instance identifier actually sent
     * @return 原始短期准入 JWT / raw short-lived admission JWT
     */
    private String admissionTicket(
            DdcServiceKey serviceKey,
            String instanceId) {
        return admissionTickets.getTicket(
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                instanceId
        ).value();
    }

    /**
     * 返回租约注册时保存的服务键。
     * / Returns the service key retained when the lease was registered.
     *
     * @param lease Provider 租约身份 / Provider lease identity
     * @return 活动服务键 / active service key
     */
    private DdcServiceKey requireActiveService(
            RpcProviderLeaseIdentity lease) {
        DdcServiceKey serviceKey = activeServices.get(lease.leaseId());
        if (serviceKey == null) {
            throw new IllegalStateException(
                    "RPC Provider lease is not registered in DDC"
            );
        }
        return serviceKey;
    }
}
