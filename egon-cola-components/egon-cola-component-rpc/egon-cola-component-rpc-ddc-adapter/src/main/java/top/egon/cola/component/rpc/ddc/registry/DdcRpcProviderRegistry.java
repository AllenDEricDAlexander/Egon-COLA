package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** RPC Provider 中立注册 SPI 到 DDC 注册 Port 的桥接。 / Bridge from neutral RPC Provider registration to DDC. */
public final class DdcRpcProviderRegistry implements RpcProviderRegistry {

    private final DdcServiceRegistryClient client;
    private final String bizCode;
    private final String appCode;
    /** IdP OAuth2 Client facade used for RPC Provider registration and heartbeats. */
    private final IdpServiceOAuth2Client serviceClient;

    /** IdP client registration and DDC resource settings. */
    private final IdpStarterProperties idpProperties;

    /** 按活动租约保存服务键，不保存原始 Token。 / Holds service keys by active lease and never stores raw tokens. */
    private final ConcurrentMap<String, DdcServiceKey> activeServices =
            new ConcurrentHashMap<>();

    /**
     * 创建 RPC Provider 到 DDC 的 SERVICE Token 注册桥接器。
     * / Creates the SERVICE-token registration bridge from RPC Provider to DDC.
     *
     * @param client DDC 服务注册客户端 / DDC service-registry client
     * @param bizCode 业务域编码 / business-domain code
     * @param appCode 应用编码 / application code
     * @param serviceClient IdP OAuth2 Client facade / IdP OAuth2 Client facade
     * @param idpProperties IdP client settings / IdP client settings
     */
    public DdcRpcProviderRegistry(
            DdcServiceRegistryClient client,
            String bizCode,
            String appCode,
            IdpServiceOAuth2Client serviceClient,
            IdpStarterProperties idpProperties) {
        this.client = Objects.requireNonNull(client, "client");
        this.bizCode = bizCode;
        this.appCode = appCode;
        this.serviceClient = Objects.requireNonNull(
                serviceClient,
                "serviceClient"
        );
        this.idpProperties = Objects.requireNonNull(
                idpProperties,
                "idpProperties"
        );
    }

    /**
     * 使用精确物理身份和新鲜票据注册 RPC Provider。
     * / Registers an RPC Provider with its exact physical identity and a fresh SERVICE token.
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
                registrationToken()
        ));
        activeServices.put(session.leaseId(), serviceKey);
        return new RpcProviderLease(
                session.instanceId(), session.leaseId(),
                session.registeredAt(), session.leaseExpireAt()
        );
    }

    /**
     * 为活动 Provider 租约携带新鲜票据续约。
     * / Renews an active Provider lease with a fresh SERVICE token.
     */
    @Override
    public RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease) {
        DdcServiceKey serviceKey = requireActiveService(lease);
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(serviceKey);
        request.setInstanceId(lease.instanceId());
        request.setLeaseId(lease.leaseId());
        request.setRegistrationToken(registrationToken());
        DdcLeaseOperationResult result = client.heartbeat(request);
        if (!result.renewed()) {
            activeServices.remove(lease.leaseId());
        }
        return operation(result);
    }

    /**
     * 仅凭租约身份注销，不要求重新取得 SERVICE Token。
     * / Deregisters with lease identity alone and does not acquire another SERVICE token.
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
     * 为 RPC Provider 取得 DDC PLATFORM SERVICE Token。
     * / Obtains a DDC PLATFORM SERVICE token for the RPC Provider.
     *
     * @return 不透明 SERVICE access token / opaque SERVICE access token
     */
    private String registrationToken() {
        IdpStarterProperties.ServiceClient client =
                idpProperties.getServiceClient();
        client.validate();
        URI audience = Objects.requireNonNull(
                idpProperties.getResourceUri(),
                "egon.cola.platform.idp.resource-uri"
        );
        return serviceClient.authorize(new IdpServiceTokenRequest(
                client.getRegistrationId(),
                client.getAppId(),
                audience,
                ServiceTokenContext.PLATFORM,
                null,
                Set.of("ddc:registration:write")
        )).getTokenValue();
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
