package top.egon.cola.component.accessguard.core;

import java.time.Duration;
import java.util.Objects;

public record GuardOutcome(
        GuardOutcomeType type,
        GuardDecision decision,
        GuardResolution resolution,
        String ruleId,
        String policy,
        long planVersion,
        String storage,
        String engine,
        Duration elapsed,
        Duration retryAfter,
        GuardFailure failure
) {

    public GuardOutcome {
        type = Objects.requireNonNull(type, "type");
        decision = Objects.requireNonNull(decision, "decision");
        resolution = Objects.requireNonNull(resolution, "resolution");
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (planVersion < 0) {
            throw new IllegalArgumentException("planVersion must not be negative");
        }
        ruleId = ruleId.trim();
        policy = policy == null ? "" : policy;
        storage = storage == null ? "" : storage;
        engine = engine == null ? "" : engine;
        elapsed = nonNegative(elapsed, "elapsed");
        retryAfter = nonNegative(retryAfter, "retryAfter");
    }

    public static GuardOutcome of(
            GuardOutcomeType type,
            GuardDecision decision,
            GuardResolution resolution,
            String ruleId,
            String policy,
            long planVersion,
            Duration elapsed
    ) {
        return new GuardOutcome(
                type, decision, resolution, ruleId, policy, planVersion,
                "", "", elapsed, Duration.ZERO, null);
    }

    public static GuardOutcome allowed(String ruleId, long planVersion) {
        return of(
                GuardOutcomeType.ALLOWED,
                GuardDecision.PASS,
                GuardResolution.NONE,
                ruleId,
                "",
                planVersion,
                Duration.ZERO);
    }

    public static GuardOutcome rejected(
            String ruleId,
            GuardDecision decision,
            String policy,
            long planVersion
    ) {
        return of(
                GuardOutcomeType.REJECTED,
                decision,
                GuardResolution.THROWN,
                ruleId,
                policy,
                planVersion,
                Duration.ZERO);
    }

    private static Duration nonNegative(Duration duration, String name) {
        Duration value = duration == null ? Duration.ZERO : duration;
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
