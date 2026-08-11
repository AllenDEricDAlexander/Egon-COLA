package top.egon.cola.platform.idp.core.token;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 只在 IdP 内部使用并绑定一个 Resource Server 的 Refresh Token 声明。
 * Internal-only Refresh Token claims bound to exactly one Resource Server.
 *
 * @param subject 用户主体 / user subject
 * @param tenantId 租户标识 / tenant identifier
 * @param sessionId 会话标识 / session identifier
 * @param clientId Client 标识 / Client identifier
 * @param familyId Refresh Family 标识 / Refresh Family identifier
 * @param tokenId 当前 Refresh Token 标识 / current Refresh Token identifier
 * @param generation 轮换代数 / rotation generation
 * @param tokenVersion 用户安全状态版本 / user security-state version
 * @param resourceServerId Resource Server 标识 / Resource Server identifier
 * @param resourceUri 唯一 Resource URI / sole Resource URI
 * @param resourceVersion Resource Server 版本 / Resource Server version
 * @param nonce 原始授权 Nonce / original authorization nonce
 * @param issuedAt 签发时间 / issuance time
 * @param expiresAt Family 过期时间 / family expiration time
 */
public record RefreshTokenClaims(
        String subject,
        String tenantId,
        String sessionId,
        String clientId,
        String familyId,
        String tokenId,
        long generation,
        long tokenVersion,
        String resourceServerId,
        String resourceUri,
        long resourceVersion,
        String nonce,
        Instant issuedAt,
        Instant expiresAt) {

    /** 校验并规范化 Refresh Token 声明。 / Validates and normalizes Refresh Token claims. */
    public RefreshTokenClaims {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        familyId = required(familyId, "familyId");
        tokenId = required(tokenId, "tokenId");
        resourceServerId = required(resourceServerId, "resourceServerId");
        resourceUri = resource(required(resourceUri, "resourceUri"));
        nonce = required(nonce, "nonce");
        if (generation < 0L || tokenVersion < 0L || resourceVersion < 0L) {
            throw new IllegalArgumentException("refresh versions must not be negative");
        }
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("refresh token is expired");
        }
    }

    /**
     * 校验绝对且无 Fragment 的 Resource URI。
     * Validates an absolute fragment-free Resource URI.
     *
     * @param value 原始 Resource URI / raw Resource URI
     * @return 规范化 Resource URI / normalized Resource URI
     */
    private static String resource(String value) {
        URI uri = URI.create(value);
        if (!uri.isAbsolute() || uri.getScheme() == null
                || uri.getScheme().isBlank() || uri.getFragment() != null
                || !uri.equals(uri.normalize())) {
            throw new IllegalArgumentException("invalid Resource URI");
        }
        return uri.toString();
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
