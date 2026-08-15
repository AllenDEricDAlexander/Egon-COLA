package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * User-scoped authorization state used while replacing active roots.
 * Authentication strength is an IdP concern and is intentionally absent.
 */
public record UserAuthorizationStateVO(
        String tenantId,
        String userId,
        Map<String, Set<String>> rootsByApplication,
        long authVersion,
        long policyVersion,
        String snapshotChecksum,
        boolean activationRequired,
        Instant expiresAt) {

    public UserAuthorizationStateVO {
        tenantId = required(tenantId, "tenantId");
        userId = required(userId, "userId");
        rootsByApplication = Map.copyOf(Objects.requireNonNull(rootsByApplication, "rootsByApplication"));
        rootsByApplication.forEach((application, roots) -> {
            required(application, "application");
            Objects.requireNonNull(roots, "roots");
        });
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("authorization versions must not be negative");
        }
        snapshotChecksum = required(snapshotChecksum, "snapshotChecksum");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
