package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;

import java.time.Instant;
import java.util.List;

public record RoleActivationInput(
        String tenantId,
        String userId,
        List<String> requestedRoleIds,
        List<EligibleAssignmentFact> assignments,
        RoleHierarchy hierarchy,
        List<DsdSetFact> dsdSets,
        AuthorizationRuleFacts authorizationFacts,
        long authVersion,
        long policyVersion,
        Instant databaseNow
) {

    public RoleActivationInput {
        tenantId = required(tenantId, "tenantId");
        userId = required(userId, "userId");
        requestedRoleIds = List.copyOf(requestedRoleIds);
        assignments = List.copyOf(assignments);
        dsdSets = List.copyOf(dsdSets);
        if (hierarchy == null || authorizationFacts == null || databaseNow == null) {
            throw new IllegalArgumentException("hierarchy, authorizationFacts and databaseNow are required");
        }
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
