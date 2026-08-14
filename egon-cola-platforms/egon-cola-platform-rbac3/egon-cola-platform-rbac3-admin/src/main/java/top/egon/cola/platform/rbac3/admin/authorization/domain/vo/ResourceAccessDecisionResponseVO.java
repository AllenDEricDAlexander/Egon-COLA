package top.egon.cola.platform.rbac3.admin.authorization.domain.vo;

import top.egon.cola.platform.rbac3.contract.authorization.Decision;

import java.time.Instant;
import java.util.Objects;

/**
 * Minimal application-entry decision without server-side identity state.
 */
public record ResourceAccessDecisionResponseVO(
        Decision decision,
        String reasonCode,
        Long authVersion,
        Long policyVersion,
        Instant decidedAt) {

    public ResourceAccessDecisionResponseVO {
        decision = Objects.requireNonNull(decision, "decision");
        reasonCode = required(reasonCode, "reasonCode");
        decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
        int versions = (authVersion == null ? 0 : 1) + (policyVersion == null ? 0 : 1);
        if (versions != 0 && versions != 2) {
            throw new IllegalArgumentException("authorization versions must be all present or absent");
        }
        if ((authVersion != null && authVersion < 0)
                || (policyVersion != null && policyVersion < 0)) {
            throw new IllegalArgumentException("authorization versions must not be negative");
        }
    }

    public static ResourceAccessDecisionResponseVO from(ResourceAccessDecisionVO result) {
        Objects.requireNonNull(result, "result");
        return new ResourceAccessDecisionResponseVO(
                result.decision(), result.reasonCode(), result.authVersion(),
                result.policyVersion(), result.decidedAt());
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
