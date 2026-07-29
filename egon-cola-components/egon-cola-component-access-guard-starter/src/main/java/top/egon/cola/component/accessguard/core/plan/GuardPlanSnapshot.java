package top.egon.cola.component.accessguard.core.plan;

import java.time.Instant;
import java.util.Objects;

public record GuardPlanSnapshot(
        String ruleId,
        long version,
        Instant loadedAt,
        String source,
        GuardPlan plan,
        String configurationFingerprint
) {

    public GuardPlanSnapshot {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        ruleId = ruleId.trim();
        loadedAt = Objects.requireNonNull(loadedAt, "loadedAt");
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        source = source.trim();
        plan = Objects.requireNonNull(plan, "plan");
        if (configurationFingerprint == null || configurationFingerprint.isBlank()) {
            throw new IllegalArgumentException("configurationFingerprint must not be blank");
        }
        configurationFingerprint = configurationFingerprint.trim();
    }
}
