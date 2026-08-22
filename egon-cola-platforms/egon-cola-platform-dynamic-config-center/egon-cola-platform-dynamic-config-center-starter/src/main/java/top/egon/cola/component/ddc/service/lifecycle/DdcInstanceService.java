package top.egon.cola.component.ddc.service.lifecycle;

import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;

import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.extension.DdcInstanceMetadataContributor;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 负责 DDC 配置客户端实例的注册、心跳、下线和元数据组装。
 * Handles registration, heartbeat, offline operations, and metadata assembly for a DDC configuration-client instance.
 */
public class DdcInstanceService {

    /**
     * DDC 客户端配置。 DDC client configuration.
     */
    private final DdcProperties properties;

    /**
     * 与 DDC 管理端通信的客户端。 Client communicating with the DDC administration endpoint.
     */
    private final DdcConfigClient adminClient;

    /**
     * 当前进程的稳定实例身份。 Stable identity of the current process.
     */
    private final DdcInstanceIdentity identity;

    /**
     * 当前租约会话持有器。 Holder of the current lease session.
     */
    private final DdcLeaseSessionHolder sessionHolder;

    /**
     * 自定义实例元数据贡献器的不可变列表。 Immutable list of custom instance metadata contributors.
     */
    private final List<DdcInstanceMetadataContributor> metadataContributors;

    /** IdP OAuth2 Client facade for configuration-client SERVICE tokens. */
    private final IdpServiceOAuth2Client serviceClient;

    /** IdP client registration and resource settings. */
    private final IdpStarterProperties idpProperties;

    /**
     * 创建支持自定义实例元数据的实例服务。
     * Creates an instance service supporting custom instance metadata.
     *
     * @param properties           DDC 客户端配置; DDC client configuration
     * @param adminClient          DDC 管理端客户端; DDC administration client
     * @param identity             实例身份; instance identity
     * @param sessionHolder        租约会话持有器; lease session holder
     * @param metadataContributors 元数据贡献器; metadata contributors
     * @param serviceClient        IdP OAuth2 Client facade; IdP OAuth2 Client facade
     * @param idpProperties        IdP client settings; IdP client settings
     */
    public DdcInstanceService(
            DdcProperties properties,
            DdcConfigClient adminClient,
            DdcInstanceIdentity identity,
            DdcLeaseSessionHolder sessionHolder,
            List<DdcInstanceMetadataContributor> metadataContributors,
            IdpServiceOAuth2Client serviceClient,
            IdpStarterProperties idpProperties) {
        this.properties = properties;
        this.adminClient = adminClient;
        this.identity = identity;
        this.sessionHolder = sessionHolder;
        this.metadataContributors = List.copyOf(metadataContributors);
        this.serviceClient = java.util.Objects.requireNonNull(
                serviceClient, "serviceClient");
        this.idpProperties = java.util.Objects.requireNonNull(
                idpProperties, "idpProperties");
    }

    /**
     * 向管理端注册实例、校验返回租约并保存当前会话。
     * Registers the instance, validates the returned lease, and stores the current session.
     *
     * @return 已校验的配置客户端租约会话; validated configuration-client lease session
     * @throws DdcException 管理端返回无效租约时抛出; thrown when the administration endpoint returns an invalid lease
     */
    public DdcLeaseSession register() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        fill(request);
        request.setRegistrationToken(registrationToken());
        request.setLeaseSeconds(properties.getInstance().getLeaseSeconds());
        request.setHeartbeatIntervalSeconds(properties.getInstance().getHeartbeatIntervalSeconds());
        DdcLeaseSession session = adminClient.register(request);
        validate(session);
        sessionHolder.replace(session);
        return session;
    }

    /**
     * 为指定租约发送心跳续约请求。
     * Sends a heartbeat renewal request for the specified lease.
     *
     * @param session 当前租约会话; current lease session
     * @return 租约操作结果; lease operation result
     */
    public DdcLeaseOperationResult heartbeat(DdcLeaseSession session) {
        DdcHeartbeatRequest request = operationRequest(session);
        request.setRegistrationToken(registrationToken());
        return adminClient.heartbeat(request);
    }

    /**
     * 为指定租约发送实例下线请求。
     * Sends an instance-offline request for the specified lease.
     *
     * @param session 当前租约会话; current lease session
     * @return 租约操作结果; lease operation result
     */
    public DdcLeaseOperationResult offline(DdcLeaseSession session) {
        return adminClient.offline(operationRequest(session));
    }

    /**
     * 返回当前进程的实例身份。
     * Returns the instance identity of the current process.
     *
     * @return 实例身份; instance identity
     */
    public DdcInstanceIdentity identity() {
        return identity;
    }

    /**
     * 使用租约与当前实例身份构造心跳或下线请求。
     * Builds a heartbeat or offline request from the lease and current instance identity.
     *
     * @param session 当前租约会话; current lease session
     * @return 已填充的租约操作请求; populated lease operation request
     */
    private DdcHeartbeatRequest operationRequest(DdcLeaseSession session) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setLeaseId(session.leaseId());
        fill(request);
        return request;
    }

    /**
     * 为当前实例的精确业务、应用和环境身份取得 PLATFORM SERVICE Token。
     * Acquires a PLATFORM SERVICE token for the current instance's exact business, application, and environment identity.
     *
     * @return 不透明 SERVICE access token; opaque SERVICE access token
     */
    private String registrationToken() {
        IdpStarterProperties.ServiceClient client = idpProperties.getServiceClient();
        client.validate();
        URI resource = java.util.Objects.requireNonNull(
                idpProperties.getResourceUri(),
                "egon.cola.platform.idp.resource-uri");
        return serviceClient.authorize(new IdpServiceTokenRequest(
                client.getRegistrationId(),
                client.getAppId(),
                resource,
                ServiceTokenContext.PLATFORM,
                null,
                java.util.Set.of("ddc:registration:write")
        )).getTokenValue();
    }

    /**
     * 将实例身份和自定义元数据填入租约操作请求。
     * Fills a lease-operation request with instance identity and custom metadata.
     *
     * @param request 待填充请求; request to populate
     */
    private void fill(DdcHeartbeatRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setBizCode(identity.bizCode());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
        request.setMetadata(metadata());
    }

    /**
     * 将实例身份和自定义元数据填入注册请求。
     * Fills a registration request with instance identity and custom metadata.
     *
     * @param request 待填充请求; request to populate
     */
    private void fill(DdcInstanceRegisterRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setBizCode(identity.bizCode());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
        request.setMetadata(metadata());
    }

    /**
     * 合并并校验所有元数据贡献器的结果。
     * Merges and validates results from all metadata contributors.
     *
     * @return 不可变元数据映射; immutable metadata map
     * @throws DdcException 元数据数量、键或值不符合限制时抛出; thrown when metadata count, keys, or values violate limits
     */
    private Map<String, String> metadata() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        metadataContributors.forEach(contributor -> {
            Map<String, String> values = contributor.metadata();
            if (values != null) {
                values.forEach((key, value) -> result.put(
                        validatedKey(key),
                        validatedValue(key, value)
                ));
            }
        });
        if (result.size() > 32) {
            throw new DdcException(
                    "DDC instance metadata must contain at most 32 entries"
            );
        }
        return Map.copyOf(result);
    }

    /**
     * 校验元数据键长度并拒绝可能暴露敏感信息的键名。
     * Validates metadata key length and rejects names that may expose sensitive information.
     *
     * @param key 元数据键; metadata key
     * @return 原始有效键; original valid key
     * @throws DdcException 键无效或具有敏感语义时抛出; thrown when the key is invalid or sensitive
     */
    private String validatedKey(String key) {
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new DdcException("Invalid DDC instance metadata key");
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("privatekey")
                || lower.contains("private-key")
                || lower.contains("certificate")) {
            throw new DdcException(
                    "DDC instance metadata key may expose sensitive data"
            );
        }
        return key;
    }

    /**
     * 将空值归一化为空字符串并校验元数据值长度。
     * Normalizes null to an empty string and validates metadata value length.
     *
     * @param key   对应元数据键，用于错误信息; associated metadata key used in errors
     * @param value 元数据值; metadata value
     * @return 归一化后的值; normalized value
     * @throws DdcException 值超过长度限制时抛出; thrown when the value exceeds the length limit
     */
    private String validatedValue(String key, String value) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > 512) {
            throw new DdcException(
                    "DDC instance metadata value is too long: " + key
            );
        }
        return normalized;
    }

    /**
     * 校验管理端返回的是当前实例的有效配置客户端租约。
     * Validates that the administration endpoint returned a valid configuration-client lease for this instance.
     *
     * @param session 待校验租约会话; lease session to validate
     * @throws DdcException 租约缺失、身份不匹配或角色错误时抛出; thrown when the lease is missing, mismatched, or has the wrong role
     */
    private void validate(DdcLeaseSession session) {
        if (session == null
                || !identity.instanceId().equals(session.instanceId())
                || session.leaseId() == null
                || session.leaseId().isBlank()
                || session.role() != DdcLeaseRole.CONFIG_CLIENT) {
            throw new DdcException("Admin returned an invalid DDC lease");
        }
    }
}
