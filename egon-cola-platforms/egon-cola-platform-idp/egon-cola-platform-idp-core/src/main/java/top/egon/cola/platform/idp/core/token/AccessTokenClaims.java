package top.egon.cola.platform.idp.core.token;

import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.PrincipalType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 平台级 USER Access Token 的无状态可信声明。
 * Stateless trusted claims for a platform USER access token.
 *
 * @param principalType 主体类型 / principal type
 * @param subject 主体标识 / subject identifier
 * @param tenantId 租户标识 / tenant identifier
 * @param tokenId JWT ID / JWT ID
 * @param audience 平台 Audience / platform audience
 * @param issuedAt 签发时间 / issuance time
 * @param notBefore 生效时间 / not-before time
 * @param expiresAt 过期时间 / expiration time
 */
public record AccessTokenClaims(
        PrincipalType principalType,
        String subject,
        String tenantId,
        String tokenId,
        String audience,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt,
        AuthenticationContext authenticationContext) {

    /**
     * 校验单 Resource 用户 Token 声明。
     * Validates single-Resource user-token claims.
     */
    public AccessTokenClaims {
        principalType = Objects.requireNonNull(principalType, "principalType");
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        tokenId = required(tokenId, "tokenId");
        audience = required(audience, "audience");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        authenticationContext = Objects.requireNonNull(
                authenticationContext,
                "authenticationContext"
        );
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
        values.put("jti", tokenId);
        values.put("aud", audience);
        values.put("acr", authenticationContext.acr());
        values.put("auth_time", authenticationContext.authTime());
        values.put("iat", issuedAt);
        values.put("nbf", notBefore);
        values.put("exp", expiresAt);
        return Map.copyOf(values);
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
