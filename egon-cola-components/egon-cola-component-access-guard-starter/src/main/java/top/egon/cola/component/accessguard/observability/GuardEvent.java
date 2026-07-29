package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.util.Objects;

public record GuardEvent(
        GuardOutcome outcome,
        boolean metricsEnabled,
        boolean loggingEnabled
) {

    public GuardEvent {
        outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public GuardEvent(GuardOutcome outcome) {
        this(outcome, true, true);
    }
}
