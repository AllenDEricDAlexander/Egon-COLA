package top.egon.cola.platform.rbac3.contract.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AuthorizationFenceDecision(
        Decision decision,
        String reasonCode,
        String tenantId,
        String subjectId,
        String permissionCode,
        String snapshotChecksum,
        String businessResource,
        String businessId,
        String traceId,
        long authVersion,
        long policyVersion,
        List<String> evidenceIds,
        Instant decidedAt,
        Instant verifiedAt
) {

    public AuthorizationFenceDecision {
        decision = Objects.requireNonNull(decision, "decision");
        reasonCode = required(reasonCode, "reasonCode");
        tenantId = required(tenantId, "tenantId");
        subjectId = required(subjectId, "subjectId");
        permissionCode = required(permissionCode, "permissionCode");
        snapshotChecksum = required(
                snapshotChecksum,
                "snapshotChecksum"
        );
        businessResource = required(
                businessResource,
                "businessResource"
        );
        businessId = required(businessId, "businessId");
        traceId = required(traceId, "traceId");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        evidenceIds = List.copyOf(Objects.requireNonNull(
                evidenceIds,
                "evidenceIds"
        ));
        evidenceIds.forEach(id -> required(id, "evidenceIds"));
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
        verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
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
