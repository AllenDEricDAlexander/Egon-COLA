package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record DataScopeDecision(
        Decision decision,
        String reasonCode,
        String tenantId,
        String subjectId,
        String permissionCode,
        String scopeType,
        boolean allInTenant,
        Set<String> allowedOrgIds,
        boolean includeOrgDescendants,
        Set<String> allowedDeptIds,
        boolean includeDeptDescendants,
        Set<String> allowedUserIds,
        boolean includeSelf,
        String selfUserId,
        String directorySnapshotVersion,
        long decisionVersion,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        List<String> evidenceIds,
        Instant decidedAt
) {

    public DataScopeDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reasonCode = required(reasonCode, "reasonCode");
        tenantId = required(tenantId, "tenantId");
        subjectId = required(subjectId, "subjectId");
        permissionCode = required(permissionCode, "permissionCode");
        scopeType = required(scopeType, "scopeType");
        allowedOrgIds = immutableIds(allowedOrgIds, "allowedOrgIds");
        allowedDeptIds = immutableIds(allowedDeptIds, "allowedDeptIds");
        allowedUserIds = immutableIds(allowedUserIds, "allowedUserIds");
        selfUserId = optional(selfUserId, "selfUserId");
        if (includeSelf && selfUserId == null) {
            throw new IllegalArgumentException(
                    "selfUserId is required when includeSelf is true"
            );
        }
        if (decision == Decision.ALLOW
                && ("NONE".equals(scopeType)
                || !hasConcreteScope(
                        allInTenant,
                        allowedOrgIds,
                        allowedDeptIds,
                        allowedUserIds,
                        includeSelf))) {
            throw new IllegalArgumentException(
                    "ALLOW requires a concrete data scope"
            );
        }
        directorySnapshotVersion = required(
                directorySnapshotVersion,
                "directorySnapshotVersion"
        );
        nonNegative(decisionVersion, "decisionVersion");
        nonNegative(authVersion, "authVersion");
        nonNegative(sessionVersion, "sessionVersion");
        nonNegative(policyVersion, "policyVersion");
        evidenceIds = List.copyOf(Objects.requireNonNull(
                evidenceIds,
                "evidenceIds"
        ));
        evidenceIds.forEach(id -> required(id, "evidenceIds"));
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
    }

    private static Set<String> immutableIds(
            Set<String> values,
            String fieldName) {
        Set<String> copy = Set.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        copy.forEach(value -> required(value, fieldName));
        return copy;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    private static boolean hasConcreteScope(
            boolean allInTenant,
            Set<String> allowedOrgIds,
            Set<String> allowedDeptIds,
            Set<String> allowedUserIds,
            boolean includeSelf) {
        return allInTenant
                || !allowedOrgIds.isEmpty()
                || !allowedDeptIds.isEmpty()
                || !allowedUserIds.isEmpty()
                || includeSelf;
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
