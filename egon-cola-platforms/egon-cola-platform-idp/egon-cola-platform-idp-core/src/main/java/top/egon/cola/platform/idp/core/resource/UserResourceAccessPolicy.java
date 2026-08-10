package top.egon.cola.platform.idp.core.resource;

import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;

import java.net.URI;
import java.util.Objects;

/**
 * USER 获取一个目标 Resource Token 前必须通过的统一入口策略。
 *
 * <p>Unified entry policy a USER must pass before receiving a target Resource Token.</p>
 */
public final class UserResourceAccessPolicy {

    /**
     * Resource 和 Grant 查询端口。
     *
     * <p>Resource and Grant lookup port.</p>
     */
    private final ResourceServerStore resources;

    /**
     * 用户租户成员关系端口。
     *
     * <p>User tenant-membership port.</p>
     */
    private final TenantMembershipPort memberships;

    /**
     * RBAC3 用户入口决策端口。
     *
     * <p>RBAC3 USER entry-decision port.</p>
     */
    private final UserResourceAccessAuthorizationPort authorization;

    /**
     * 创建 USER Resource 入口策略。
     *
     * <p>Creates the USER Resource entry policy.</p>
     *
     * @param resources     Resource 和 Grant 查询端口；Resource and Grant lookup port
     * @param memberships   租户成员关系端口；tenant-membership port
     * @param authorization RBAC3 入口决策端口；RBAC3 entry-decision port
     */
    public UserResourceAccessPolicy(
            ResourceServerStore resources,
            TenantMembershipPort memberships,
            UserResourceAccessAuthorizationPort authorization) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.memberships = Objects.requireNonNull(
                memberships,
                "memberships"
        );
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    /**
     * 按固定顺序校验 Client、Resource、Grant、成员关系和入口权限。
     *
     * <p>Checks the Client, Resource, Grant, membership, and entry permission in a fixed order.</p>
     *
     * @param client      请求 Token 的 OAuth Client；OAuth Client requesting the token
     * @param resourceUri 目标 Resource URI；target Resource URI
     * @param identitySub 用户身份标识；user identity subject
     * @param tenantId    当前租户；current tenant
     * @param sessionId   当前身份会话；current identity session
     * @return 已授权 USER Resource 上下文；authorized USER Resource context
     */
    public UserResourceAccess authorize(
            OAuthClient client,
            URI resourceUri,
            String identitySub,
            String tenantId,
            String sessionId) {
        Objects.requireNonNull(client, "client");
        if (client.status() != OAuthClient.Status.ACTIVE) {
            deny("IDP_CLIENT_DISABLED", "OAuth Client is disabled");
        }
        ResourceServer resource = resources.findByUri(
                        Objects.requireNonNull(resourceUri, "resourceUri")
                )
                .orElseThrow(() -> new ResourceAuthorizationException(
                        "IDP_RESOURCE_SERVER_NOT_FOUND",
                        "Resource Server was not found"
                ));
        if (!resource.active()) {
            deny(
                    "IDP_RESOURCE_SERVER_DISABLED",
                    "Resource Server is disabled"
            );
        }
        ClientResourceGrant grant = resources.findGrant(
                        client.clientId(),
                        resource.resourceServerId(),
                        ResourceGrantType.USER_DELEGATION,
                        null
                )
                .filter(ClientResourceGrant::active)
                .orElseThrow(() -> new ResourceAuthorizationException(
                        "IDP_USER_RESOURCE_GRANT_NOT_FOUND",
                        "USER Resource grant was not found"
                ));
        TenantMembershipPort.TenantMembership membership =
                memberships.resolve(
                        required(identitySub, "identitySub"),
                        required(tenantId, "tenantId"),
                        client.clientId()
                );
        if (membership.status()
                != TenantMembershipPort.MembershipStatus.ACTIVE) {
            deny("IDP_TENANT_MEMBERSHIP_DISABLED", "Membership is disabled");
        }
        UserResourceAccessAuthorizationPort.AccessDecision decision =
                authorization.decide(
                        new UserResourceAccessAuthorizationPort.AccessRequest(
                                membership.identitySub(),
                                membership.tenantId(),
                                required(sessionId, "sessionId"),
                                resource.rbacApplicationCode(),
                                resource.entryPermissionCode()
                        )
                );
        if (decision.decision()
                != UserResourceAccessAuthorizationPort.Decision.ALLOW) {
            deny("IDP_RESOURCE_ACCESS_DENIED", decision.reason());
        }
        return new UserResourceAccess(
                resource.resourceServerId(),
                resource.resourceUri(),
                resource.version(),
                membership.rbac3UserId(),
                grant.version(),
                decision.authorizationVersion(),
                decision.contextVersion(),
                decision.policyVersion()
        );
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
     * USER Resource 授权结果。
     *
     * <p>Authorized USER Resource result.</p>
     *
     * @param resourceServerId    Resource Server 标识；Resource Server identifier
     * @param resourceUri         Resource URI；Resource URI
     * @param resourceVersion     Resource 版本；Resource version
     * @param rbac3UserId         RBAC3 用户标识；RBAC3 user identifier
     * @param grantVersion        Client Grant 版本；Client Grant version
     * @param authorizationVersion RBAC3 授权版本；RBAC3 authorization version
     * @param contextVersion      RBAC3 上下文版本；RBAC3 context version
     * @param policyVersion       RBAC3 策略版本；RBAC3 policy version
     */
    public record UserResourceAccess(
            String resourceServerId,
            URI resourceUri,
            long resourceVersion,
            String rbac3UserId,
            long grantVersion,
            long authorizationVersion,
            long contextVersion,
            long policyVersion
    ) {
    }
}
