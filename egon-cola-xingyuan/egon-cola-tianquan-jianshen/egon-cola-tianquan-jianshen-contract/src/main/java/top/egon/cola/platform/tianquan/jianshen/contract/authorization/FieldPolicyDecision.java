package top.egon.cola.platform.tianquan.jianshen.contract.authorization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FieldPolicyDecision(
        @NotNull Decision decision,
        @NotBlank String reasonCode,
        @NotBlank String tenantId,
        @NotBlank String subjectId,
        @NotBlank String permissionCode,
        @NotBlank String applicationCode,
        @NotBlank String resourceCode,
        @NotNull Map<@NotBlank String, @NotNull @Valid FieldAccess> fields,
        @PositiveOrZero long authVersion,
        @PositiveOrZero long policyVersion,
        @NotNull List<@NotBlank String> evidenceIds,
        @NotNull Instant decidedAt
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
        nonNegative(policyVersion, "policyVersion");
        evidenceIds = List.copyOf(Objects.requireNonNull(
                evidenceIds,
                "evidenceIds"
        ));
        evidenceIds.forEach(id -> required(id, "evidenceIds"));
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
    }

    public record FieldAccess(
            @NotNull FieldAccessLevel level,
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
