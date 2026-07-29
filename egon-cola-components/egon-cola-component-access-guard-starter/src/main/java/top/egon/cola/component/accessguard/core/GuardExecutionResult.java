package top.egon.cola.component.accessguard.core;

import java.util.Objects;

public record GuardExecutionResult<T>(T value, GuardOutcome outcome) {

    public GuardExecutionResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
    }
}
