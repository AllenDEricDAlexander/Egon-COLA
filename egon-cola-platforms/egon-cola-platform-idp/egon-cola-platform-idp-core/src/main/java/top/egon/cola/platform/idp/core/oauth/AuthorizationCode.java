package top.egon.cola.platform.idp.core.oauth;

import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 绑定一个用户、Client、会话和 Resource Server 的一次性授权码载荷。
 * One-time authorization-code payload bound to one user, Client, session, and Resource Server.
 *
 * @param identitySub IdP 用户主体 / IdP user subject
 * @param tenantId 租户标识 / tenant identifier
 * @param rbac3UserId RBAC3 用户标识 / RBAC3 user identifier
 * @param sessionId IdP 会话标识 / IdP session identifier
 * @param clientId OAuth Client 标识 / OAuth Client identifier
 * @param resourceUri 唯一 Resource URI / sole Resource URI
 * @param resourceServerId Resource Server 稳定标识 / stable Resource Server identifier
 * @param resourceVersion Resource Server 授权版本 / Resource Server authorization version
 * @param redirectUri 精确回调地址 / exact redirect URI
 * @param nonce Token Nonce / token nonce
 * @param codeChallenge PKCE S256 Challenge / PKCE S256 challenge
 * @param issuedAt 签发时间 / issuance time
 * @param expiresAt 过期时间 / expiration time
 */
public record AuthorizationCode(
        String identitySub,
        String tenantId,
        String rbac3UserId,
        String sessionId,
        String clientId,
        URI resourceUri,
        String resourceServerId,
        long resourceVersion,
        String redirectUri,
        String nonce,
        String codeChallenge,
        Instant issuedAt,
        Instant expiresAt) {

    /**
     * 校验并规范化授权码载荷。
     * Validates and normalizes the authorization-code payload.
     */
    public AuthorizationCode {
        identitySub = required(identitySub, "identitySub");
        tenantId = required(tenantId, "tenantId");
        rbac3UserId = required(rbac3UserId, "rbac3UserId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        resourceUri = resourceUri(resourceUri);
        resourceServerId = required(resourceServerId, "resourceServerId");
        if (resourceVersion < 0L) {
            throw new IllegalArgumentException("resourceVersion must not be negative");
        }
        redirectUri = required(redirectUri, "redirectUri");
        nonce = required(nonce, "nonce");
        codeChallenge = required(codeChallenge, "codeChallenge");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }

    /**
     * 使用换码阶段重新校验得到的最新 Resource 授权替换绑定版本。
     * Rebinds this code to the latest Resource authorization validated during exchange.
     *
     * @param access 最新用户 Resource 授权 / latest user Resource authorization
     * @return 更新 Resource 绑定后的授权码载荷 / code payload with refreshed Resource binding
     */
    public AuthorizationCode withResourceAccess(
            UserResourceAccessPolicy.UserResourceAccess access) {
        Objects.requireNonNull(access, "access");
        if (!resourceUri.equals(access.resourceUri())
                || !resourceServerId.equals(access.resourceServerId())) {
            throw new IllegalArgumentException("Resource binding does not match authorization code");
        }
        return new AuthorizationCode(identitySub, tenantId, access.rbac3UserId(),
                sessionId, clientId, resourceUri, resourceServerId,
                access.resourceVersion(), redirectUri, nonce, codeChallenge,
                issuedAt, expiresAt);
    }

    /**
     * 校验绝对且无 Fragment 的 Resource URI。
     * Validates an absolute fragment-free Resource URI.
     *
     * @param value 原始 Resource URI / raw Resource URI
     * @return 规范化 Resource URI / normalized Resource URI
     */
    private static URI resourceUri(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute() || value.getScheme() == null
                || value.getScheme().isBlank() || value.getFragment() != null) {
            throw new IllegalArgumentException(
                    "resourceUri must be an absolute URI without a fragment");
        }
        return value.normalize();
    }

    /**
     * 校验必填文本。
     * Validates required text.
     *
     * @param value 待校验文本 / text to validate
     * @param field 字段名 / field name
     * @return 已校验文本 / validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
