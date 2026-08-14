package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable authorization facts for one USER subject and system.
 */
public record SystemAuthorizationSnapshot(
        String tenantId,
        String identitySub,
        String rbac3UserId,
        String systemCode,
        long authVersion,
        long policyVersion,
        List<String> activeRoleIds,
        Set<String> permissions,
        Map<String, DataScopeDecision> dataScopes,
        Map<String, FieldPolicyDecision> fieldPolicies,
        String checksum,
        Instant generatedAt,
        Instant expiresAt
) {

    public SystemAuthorizationSnapshot {
        tenantId = required(tenantId, "tenantId");
        identitySub = required(identitySub, "identitySub");
        rbac3UserId = required(rbac3UserId, "rbac3UserId");
        systemCode = required(systemCode, "systemCode");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        activeRoleIds = List.copyOf(Objects.requireNonNull(activeRoleIds, "activeRoleIds"));
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        dataScopes = Map.copyOf(Objects.requireNonNull(dataScopes, "dataScopes"));
        fieldPolicies = Map.copyOf(Objects.requireNonNull(fieldPolicies, "fieldPolicies"));
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
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
