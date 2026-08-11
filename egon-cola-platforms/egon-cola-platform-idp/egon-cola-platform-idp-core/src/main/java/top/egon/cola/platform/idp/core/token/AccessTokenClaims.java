package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.PrincipalType;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RFC 9068 风格单 Resource Access Token 的可信声明。
 * Trusted claims for an RFC 9068-style single-Resource Access Token.
 *
 * @param principalType 主体类型 / principal type
 * @param subject 主体标识 / subject identifier
 * @param tenantId 租户标识 / tenant identifier
 * @param sessionId 用户会话标识 / user session identifier
 * @param clientId OAuth Client 标识 / OAuth Client identifier
 * @param tokenId JWT ID / JWT ID
 * @param tokenVersion 用户安全状态版本 / user security-state version
 * @param resourceVersion Resource Server 版本 / Resource Server version
 * @param audience 唯一 Resource URI Audience / sole Resource URI audience
 * @param nonce 授权请求 Nonce / authorization-request nonce
 * @param issuedAt 签发时间 / issuance time
 * @param notBefore 生效时间 / not-before time
 * @param expiresAt 过期时间 / expiration time
 */
public record AccessTokenClaims(
        PrincipalType principalType,
        String subject,
        String tenantId,
        String sessionId,
        String clientId,
        String tokenId,
        long tokenVersion,
        long resourceVersion,
        String audience,
        String nonce,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt) {

    /**
     * 校验单 Resource 用户 Token 声明。
     * Validates single-Resource user-token claims.
     */
    public AccessTokenClaims {
        principalType = Objects.requireNonNull(principalType, "principalType");
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        tokenId = required(tokenId, "tokenId");
        if (tokenVersion < 0L || resourceVersion < 0L) {
            throw new IllegalArgumentException("token versions must not be negative");
        }
        audience = resource(required(audience, "audience"));
        nonce = required(nonce, "nonce");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (notBefore.isBefore(issuedAt) || !expiresAt.isAfter(notBefore)) {
            throw new IllegalArgumentException("invalid access token time range");
        }
    }

    /**
     * 生成不包含角色、权限、数据、字段或服务 Scope 的安全声明映射。
     * Produces a safe claim map without roles, permissions, data, fields, or service scopes.
     *
     * @return 不可变声明映射 / immutable claim map
     */
    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("principal_type", principalType.name());
        values.put("sub", subject);
        values.put("tid", tenantId);
        values.put("sid", sessionId);
        values.put("client_id", clientId);
        values.put("jti", tokenId);
        values.put("token_version", tokenVersion);
        values.put("resource_version", resourceVersion);
        values.put("aud", List.of(audience));
        values.put("nonce", nonce);
        values.put("iat", issuedAt);
        values.put("nbf", notBefore);
        values.put("exp", expiresAt);
        return Map.copyOf(values);
    }

    /**
     * 校验 Resource URI Audience。
     * Validates the Resource URI audience.
     *
     * @param value 原始 Audience / raw audience
     * @return 规范化 Resource URI / normalized Resource URI
     */
    private static String resource(String value) {
        URI uri = URI.create(value);
        if (!uri.isAbsolute() || uri.getScheme() == null
                || uri.getScheme().isBlank() || uri.getFragment() != null
                || !uri.equals(uri.normalize())) {
            throw new IllegalArgumentException(
                    "audience must be an absolute Resource URI without a fragment");
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
