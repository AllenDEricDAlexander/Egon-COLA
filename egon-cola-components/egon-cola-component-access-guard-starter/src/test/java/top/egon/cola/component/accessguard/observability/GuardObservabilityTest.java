package top.egon.cola.component.accessguard.observability;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.GuardOutcomeType;
import top.egon.cola.component.accessguard.core.GuardResolution;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GuardObservabilityTest {

    @Test
    void finalizerPublishesExactlyOneFinalEventAndKeepsStageEventsSeparate() {
        RecordingListener listener = new RecordingListener();
        GuardInvocationFinalizer finalizer = new GuardInvocationFinalizer(
                new CompositeGuardEventPublisher(List.of(listener)),
                new ObservabilityConfig(true, true, true, true, true));
        GuardOutcome outcome = outcome();

        finalizer.stage("admission", outcome);
        finalizer.finish(outcome);
        finalizer.finish(GuardOutcome.allowed("draw", 7L));

        assertThat(listener.events).singleElement()
                .extracting(GuardEvent::outcome)
                .isEqualTo(outcome);
        assertThat(listener.stages).singleElement()
                .extracting(GuardStageEvent::stage)
                .isEqualTo("admission");
    }

    @Test
    void observerFailureCannotChangeDeliveryToOtherObservers() {
        RecordingListener listener = new RecordingListener();
        GuardEventListener broken = event -> {
            throw new IllegalStateException("observer failed");
        };
        GuardInvocationFinalizer finalizer = new GuardInvocationFinalizer(
                new CompositeGuardEventPublisher(List.of(broken, listener)),
                ObservabilityConfig.defaults());

        finalizer.finish(outcome());

        assertThat(listener.events).hasSize(1);
    }

    @Test
    void metricsUseOnlyBoundedTagsAndStageEventsDoNotIncrementCalls() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerGuardEventListener listener = new MicrometerGuardEventListener(registry);
        GuardEvent event = new GuardEvent(outcome(), true, true);

        listener.onStage(new GuardStageEvent("admission", outcome()));
        listener.onEvent(event);

        assertThat(registry.get("egon.access.guard.calls").counter().count()).isEqualTo(1D);
        assertThat(registry.get("egon.access.guard.duration").timer().count()).isEqualTo(1L);
        assertThat(registry.get("egon.access.guard.store.failures").counter().count()).isEqualTo(1D);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .extracting(Tag::getKey)
                .allMatch(MicrometerGuardEventListener.ALLOWED_TAGS::contains)
                .doesNotContain("key", "keyHash", "method", "exceptionMessage", "user"));
    }

    @Test
    void structuredLoggingFieldsContainNoRequestOrFailureDetails() {
        LoggingGuardEventListener listener = new LoggingGuardEventListener();

        Map<String, Object> fields = listener.fields(new GuardEvent(outcome(), true, true));

        assertThat(fields).containsKeys(
                        "ruleId", "planVersion", "policy", "type", "decision", "resolution",
                        "engine", "storage", "elapsed", "retryAfter", "failureCategory", "failureCode")
                .doesNotContainKeys("key", "keyHash", "method", "exceptionMessage", "arguments", "returnValue");
        assertThat(fields.toString()).doesNotContain("Authorization", "user-1", "raw-key");
    }

    @Test
    void planReloadAndEnabledLocalStoresUseTheirStableMetricNames() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerGuardEventListener listener = new MicrometerGuardEventListener(registry, () -> 2, () -> 3);

        listener.onPlanChanged(new GuardPlanChangedEvent("draw", 6L, 7L, "properties", Instant.EPOCH));

        assertThat(registry.get("egon.access.guard.plan.reloads").counter().count()).isEqualTo(1D);
        assertThat(registry.find("egon.access.guard.local.entries").gauges())
                .hasSize(2)
                .extracting(gauge -> gauge.value())
                .containsExactlyInAnyOrder(2D, 3D);
    }

    private static GuardOutcome outcome() {
        return new GuardOutcome(
                GuardOutcomeType.DEGRADED,
                GuardDecision.STORE_FAILED,
                GuardResolution.LOCAL_FALLBACK,
                "draw",
                "rate-limit",
                7L,
                "LOCAL",
                "AOP",
                Duration.ofMillis(12),
                Duration.ofSeconds(1),
                new GuardFailure("STORE", "OPERATION_FAILED"));
    }

    private static final class RecordingListener implements GuardEventListener {

        private final List<GuardEvent> events = new ArrayList<>();
        private final List<GuardStageEvent> stages = new ArrayList<>();

        @Override
        public void onEvent(GuardEvent event) {
            events.add(event);
        }

        @Override
        public void onStage(GuardStageEvent event) {
            stages.add(event);
        }
    }
}
