package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.Objects;

public record RefreshFamily(
        String familyId,
        String identitySub,
        String tenantId,
        String sessionId,
        String clientId,
        long tokenVersion,
        long generation,
        String currentTokenDigest,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {

    public RefreshFamily {
        familyId = required(familyId, "familyId");
        identitySub = required(identitySub, "identitySub");
        tenantId = required(tenantId, "tenantId");
        sessionId = required(sessionId, "sessionId");
        clientId = required(clientId, "clientId");
        currentTokenDigest = required(
                currentTokenDigest,
                "currentTokenDigest"
        );
        if (tokenVersion < 0L || generation < 0L) {
            throw new IllegalArgumentException(
                    "refresh versions must not be negative"
            );
        }
        status = Objects.requireNonNull(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt) || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("invalid family time range");
        }
    }

    public boolean active() {
        return status == Status.ACTIVE;
    }

    public RefreshFamily rotated(
            String successorTokenDigest,
            long successorGeneration,
            Instant now
    ) {
        if (!active() || successorGeneration != generation + 1L) {
            throw new IllegalStateException("invalid refresh rotation");
        }
        return new RefreshFamily(
                familyId,
                identitySub,
                tenantId,
                sessionId,
                clientId,
                tokenVersion,
                successorGeneration,
                successorTokenDigest,
                Status.ACTIVE,
                createdAt,
                Objects.requireNonNull(now, "now"),
                expiresAt
        );
    }

    public RefreshFamily compromised(Instant now) {
        return withStatus(Status.COMPROMISED, now);
    }

    public RefreshFamily revoked(Instant now) {
        return withStatus(Status.REVOKED, now);
    }

    private RefreshFamily withStatus(Status value, Instant now) {
        return new RefreshFamily(
                familyId,
                identitySub,
                tenantId,
                sessionId,
                clientId,
                tokenVersion,
                generation,
                currentTokenDigest,
                value,
                createdAt,
                Objects.requireNonNull(now, "now"),
                expiresAt
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public enum Status {
        ACTIVE,
        COMPROMISED,
        REVOKED
    }
}
