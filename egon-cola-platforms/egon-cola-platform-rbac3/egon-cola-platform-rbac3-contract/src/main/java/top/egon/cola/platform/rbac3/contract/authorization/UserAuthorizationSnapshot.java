package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable authorization projection bound to a USER subject, not a session.
 */
public record UserAuthorizationSnapshot(
        String systemCode,
        String tenantId,
        String identitySub,
        String rbacUserId,
        long authVersion,
        long policyVersion,
        List<AppAuthorizationContext> appContexts,
        String checksum,
        Instant generatedAt,
        Instant expiresAt
) {

    public UserAuthorizationSnapshot {
        systemCode = required(systemCode, "systemCode");
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        rbacUserId = required(rbacUserId, "rbacUserId");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        appContexts = List.copyOf(Objects.requireNonNull(appContexts, "appContexts"));
        checksum = required(checksum, "checksum");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(generatedAt)) {
            throw new IllegalArgumentException("expiresAt must be after generatedAt");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
