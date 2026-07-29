package top.egon.cola.component.accessguard.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

public final class MicrometerGuardEventListener implements GuardEventListener {

    public static final List<String> ALLOWED_TAGS = List.of(
            "ruleId", "policy", "type", "decision", "resolution", "engine", "storage");

    private final MeterRegistry registry;

    public MicrometerGuardEventListener(MeterRegistry registry) {
        this(registry, null, null);
    }

    public MicrometerGuardEventListener(
            MeterRegistry registry,
            IntSupplier penaltyEntries,
            IntSupplier rateLimitEntries
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        registerGauge("penalty-box", penaltyEntries);
        registerGauge("rate-limit", rateLimitEntries);
    }

    @Override
    public void onEvent(GuardEvent event) {
        if (!event.metricsEnabled()) {
            return;
        }
        GuardOutcome outcome = event.outcome();
        Tags tags = Tags.of(
                "ruleId", outcome.ruleId(),
                "policy", outcome.policy(),
                "type", outcome.type().name(),
                "decision", outcome.decision().name(),
                "resolution", outcome.resolution().name(),
                "engine", outcome.engine(),
                "storage", outcome.storage());
        Counter.builder("egon.access.guard.calls").tags(tags).register(registry).increment();
        Timer.builder("egon.access.guard.duration").tags(tags).register(registry)
                .record(outcome.elapsed().toNanos(), TimeUnit.NANOSECONDS);
        if (outcome.failure() != null && "STORE".equals(outcome.failure().category())) {
            Counter.builder("egon.access.guard.store.failures").tags(tags).register(registry).increment();
        }
    }

    @Override
    public void onPlanChanged(GuardPlanChangedEvent event) {
        Counter.builder("egon.access.guard.plan.reloads")
                .tag("ruleId", event.ruleId())
                .register(registry)
                .increment();
    }

    private void registerGauge(String policy, IntSupplier entries) {
        if (entries == null) {
            return;
        }
        registry.gauge(
                "egon.access.guard.local.entries",
                List.of(Tag.of("policy", policy)),
                entries,
                supplier -> supplier.getAsInt());
    }
}
