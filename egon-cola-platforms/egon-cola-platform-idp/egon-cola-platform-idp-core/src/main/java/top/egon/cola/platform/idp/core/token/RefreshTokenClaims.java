package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.Objects;

/**
 * 稳定 Refresh Token 的无状态可信声明。
 * Stateless trusted claims for a stable Refresh Token.
 *
 * @param subject 用户主体；user subject
 * @param tenantId 租户标识；tenant identifier
 * @param tokenId Refresh Token jti；Refresh Token jti
 * @param issuedAt 签发时间；issuance instant
 * @param notBefore 生效时间；not-before instant
 * @param expiresAt 绝对过期时间；absolute expiration instant
 */
public record RefreshTokenClaims(
        String subject,
        String tenantId,
        String tokenId,
        Instant issuedAt,
        Instant notBefore,
        Instant expiresAt
) {

    public RefreshTokenClaims {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        tokenId = required(tokenId, "tokenId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        notBefore = Objects.requireNonNull(notBefore, "notBefore");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (notBefore.isBefore(issuedAt) || !expiresAt.isAfter(notBefore)) {
            throw new IllegalArgumentException("invalid refresh token time range");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
