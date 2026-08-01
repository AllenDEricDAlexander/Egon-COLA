package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FieldPolicyDecision(
        Decision decision,
        String reasonCode,
        String tenantId,
        String subjectId,
        String permissionCode,
        String applicationCode,
        String resourceCode,
        Map<String, FieldAccess> fields,
        long authVersion,
        long sessionVersion,
        long policyVersion,
        List<String> evidenceIds,
        Instant decidedAt
) {

    public FieldPolicyDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reasonCode = required(reasonCode, "reasonCode");
        tenantId = required(tenantId, "tenantId");
        subjectId = required(subjectId, "subjectId");
        permissionCode = required(permissionCode, "permissionCode");
        applicationCode = required(applicationCode, "applicationCode");
        resourceCode = required(resourceCode, "resourceCode");
        fields = Map.copyOf(Objects.requireNonNull(fields, "fields"));
        fields.keySet().forEach(field -> required(field, "fields"));
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

    public record FieldAccess(
            FieldAccessLevel level,
            String maskingStrategy
    ) {

        public FieldAccess {
            level = Objects.requireNonNull(level, "level");
            maskingStrategy = optional(
                    maskingStrategy,
                    "maskingStrategy"
            );
        }
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

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
