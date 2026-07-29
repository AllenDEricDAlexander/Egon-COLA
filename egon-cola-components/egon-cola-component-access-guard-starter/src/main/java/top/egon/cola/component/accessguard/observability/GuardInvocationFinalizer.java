package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GuardInvocationFinalizer {

    private final GuardEventPublisher publisher;
    private final ObservabilityConfig config;
    private final AtomicBoolean finished = new AtomicBoolean();

    public GuardInvocationFinalizer(GuardEventPublisher publisher, ObservabilityConfig config) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean finish(GuardOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        if (config.finalEvents()) {
            try {
                publisher.publish(new GuardEvent(outcome, config.metrics(), config.logging()));
            } catch (RuntimeException ignored) {
                // A custom publisher is subject to the same fail-open observability contract.
            }
        }
        return true;
    }

    public void stage(String stage, GuardOutcome outcome) {
        if (!config.stageEvents() || finished.get()) {
            return;
        }
        try {
            publisher.publishStage(new GuardStageEvent(stage, outcome, config.logging()));
        } catch (RuntimeException ignored) {
            // Stage diagnostics are optional and never affect governance.
        }
    }
}
