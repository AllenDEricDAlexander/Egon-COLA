package top.egon.cola.platform.idp.core.token;

import java.time.Instant;

/**
 * Persisted metadata for one stable refresh token.
 * 一个稳定 Refresh Token 的持久化元数据。
 */
public record RefreshTokenRecord(
        String tokenDigest,
        String identitySub,
        String tenantId,
        Instant issuedAt,
        Instant expiresAt,
        Status status
) {

    public RefreshTokenRecord {
        if (tokenDigest == null || tokenDigest.isBlank()) {
            throw new IllegalArgumentException("tokenDigest is required");
        }
        if (identitySub == null || identitySub.isBlank()) {
            throw new IllegalArgumentException("identitySub is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("invalid refresh lifetime");
        }
        if (status == null) {
            throw new NullPointerException("status");
        }
    }

    public enum Status {
        ACTIVE,
        REVOKED,
        EXPIRED
    }
}
