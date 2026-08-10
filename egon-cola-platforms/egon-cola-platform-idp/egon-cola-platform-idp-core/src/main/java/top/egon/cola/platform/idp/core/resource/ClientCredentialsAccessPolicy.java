package top.egon.cola.platform.idp.core.resource;

import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * IdP 内部完成的 CLIENT_CREDENTIALS 目标、租户和 Scope 授权策略。
 *
 * <p>IdP-owned CLIENT_CREDENTIALS authorization policy for target, tenant, and scopes.</p>
 */
public final class ClientCredentialsAccessPolicy {

    /**
     * Resource 和 Grant 查询端口。
     *
     * <p>Resource and Grant lookup port.</p>
     */
    private final ResourceServerStore resources;

    /**
     * 创建服务访问策略。
     *
     * <p>Creates the service-access policy.</p>
     *
     * @param resources Resource 和 Grant 查询端口；Resource and Grant lookup port
     */
    public ClientCredentialsAccessPolicy(ResourceServerStore resources) {
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    /**
     * 授权一个 Confidential Client 访问单一租户下的目标 Resource。
     *
     * <p>Authorizes a Confidential Client to access a target Resource for one tenant.</p>
     *
     * @param sourceClient    源服务 Client；source service Client
     * @param targetResource  目标 Resource Server；target Resource Server
     * @param tenantId        明确租户；explicit tenant
     * @param requestedScopes 请求 Scope；requested scopes
     * @return 最小服务访问授权结果；minimal service-access authorization result
     */
    public ServiceResourceAccess authorize(
            OAuthClient sourceClient,
            ResourceServer targetResource,
            String tenantId,
            Set<String> requestedScopes) {
        Objects.requireNonNull(sourceClient, "sourceClient");
        Objects.requireNonNull(targetResource, "targetResource");
        if (sourceClient.status() != OAuthClient.Status.ACTIVE) {
            deny("IDP_CLIENT_DISABLED", "OAuth Client is disabled");
        }
        if (sourceClient.clientType()
                != OAuthClient.ClientType.CONFIDENTIAL) {
            deny(
                    "IDP_CLIENT_CREDENTIALS_UNAUTHORIZED",
                    "OAuth Client is not confidential"
            );
        }
        if (!targetResource.active()) {
            deny(
                    "IDP_RESOURCE_SERVER_DISABLED",
                    "Target Resource Server is disabled"
            );
        }
        String safeTenantId = required(tenantId, "tenantId");
        Set<String> safeScopes = scopes(requestedScopes);
        if (safeScopes.isEmpty()) {
            deny("IDP_SERVICE_SCOPE_INVALID", "Requested scope is empty");
        }
        ClientResourceGrant grant = resources.findGrant(
                        sourceClient.clientId(),
                        targetResource.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        safeTenantId
                )
                .filter(ClientResourceGrant::active)
                .orElseThrow(() -> new ResourceAuthorizationException(
                        "IDP_SERVICE_RESOURCE_GRANT_NOT_FOUND",
                        "Service Resource grant was not found"
                ));
        if (!grant.allows(safeScopes)) {
            deny(
                    "IDP_SERVICE_SCOPE_INVALID",
                    "Requested scope exceeds the Service Resource grant"
            );
        }
        ResourceServer sourceResource = resources.findByManagementClientId(
                        sourceClient.clientId()
                )
                .filter(ResourceServer::active)
                .orElseThrow(() -> new ResourceAuthorizationException(
                        "IDP_SOURCE_RESOURCE_NOT_FOUND",
                        "Source Resource Server was not found"
                ));
        return new ServiceResourceAccess(
                sourceClient.clientId(),
                sourceResource.resourceServerId(),
                sourceResource.bizCode(),
                sourceResource.appCode(),
                sourceResource.environment(),
                targetResource.resourceServerId(),
                targetResource.resourceUri(),
                targetResource.version(),
                safeTenantId,
                safeScopes,
                grant.version()
        );
    }

    /**
     * 规范化 Scope。
     *
     * <p>Normalizes scopes.</p>
     *
     * @param values 原始 Scope；raw scopes
     * @return 已排序不可变 Scope；sorted immutable scopes
     */
    private Set<String> scopes(Set<String> values) {
        Objects.requireNonNull(values, "requestedScopes");
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            normalized.add(required(value, "scope"));
        }
        return Collections.unmodifiableSet(normalized);
    }

    /**
     * 抛出稳定拒绝异常。
     *
     * <p>Throws a stable denial exception.</p>
     *
     * @param code    稳定错误码；stable error code
     * @param message 安全错误描述；safe error description
     */
    private void deny(String code, String message) {
        throw new ResourceAuthorizationException(code, message);
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验文本；text to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 已授权的服务访问上下文。
     *
     * <p>Authorized service-access context.</p>
     *
     * @param sourceClientId         源 Client；source Client
     * @param sourceResourceServerId 源 Resource；source Resource
     * @param sourceBizCode          源业务域；source business domain
     * @param sourceAppCode          源应用；source application
     * @param sourceEnvironment      源环境；source environment
     * @param targetResourceServerId 目标 Resource；target Resource
     * @param targetResourceUri      目标 Resource URI；target Resource URI
     * @param targetResourceVersion  目标 Resource 版本；target Resource version
     * @param tenantId               绑定租户；bound tenant
     * @param scopes                 本次许可 Scope；scopes granted for this request
     * @param grantVersion           Service Grant 版本；Service Grant version
     */
    public record ServiceResourceAccess(
            String sourceClientId,
            String sourceResourceServerId,
            String sourceBizCode,
            String sourceAppCode,
            String sourceEnvironment,
            String targetResourceServerId,
            URI targetResourceUri,
            long targetResourceVersion,
            String tenantId,
            Set<String> scopes,
            long grantVersion
    ) {
    }
}
