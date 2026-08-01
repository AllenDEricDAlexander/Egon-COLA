package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OperationSodDecision(
        Decision decision,
        String reasonCode,
        String tenantId,
        String subjectId,
        String permissionCode,
        String applicationCode,
        String businessResource,
        String businessId,
        String actionCode,
        List<String> conflictingActionCodes,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        List<String> evidenceIds,
        Instant decidedAt
) {

    public OperationSodDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reasonCode = required(reasonCode, "reasonCode");
        tenantId = required(tenantId, "tenantId");
        subjectId = required(subjectId, "subjectId");
        permissionCode = required(permissionCode, "permissionCode");
        applicationCode = required(applicationCode, "applicationCode");
        businessResource = required(
                businessResource,
                "businessResource"
        );
        businessId = required(businessId, "businessId");
        actionCode = required(actionCode, "actionCode");
        conflictingActionCodes = List.copyOf(Objects.requireNonNull(
                conflictingActionCodes,
                "conflictingActionCodes"
        ));
        conflictingActionCodes.forEach(code -> required(
                code,
                "conflictingActionCodes"
        ));
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

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
