package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.util.Objects;

public record GuardStageEvent(String stage, GuardOutcome outcome, boolean loggingEnabled) {

    public GuardStageEvent {
        if (stage == null || stage.isBlank()) {
            throw new IllegalArgumentException("stage must not be blank");
        }
        stage = stage.trim();
        outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public GuardStageEvent(String stage, GuardOutcome outcome) {
        this(stage, outcome, true);
    }
}
