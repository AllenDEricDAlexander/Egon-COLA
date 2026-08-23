package top.egon.cola.component.accessguard.core;

import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;

import java.util.Objects;

/**
 * Immutable handoff from admission evaluation to execution coordination.
 */
public record GuardAdmission(
        GuardOutcome outcome,
        ExecutionConfig execution,
        ObservabilityConfig observability,
        long startedAtNanos
) {

    public GuardAdmission {
        outcome = Objects.requireNonNull(outcome, "outcome");
        execution = Objects.requireNonNull(execution, "execution");
        observability = Objects.requireNonNull(observability, "observability");
    }
}
