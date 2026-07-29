package top.egon.cola.component.accessguard.core.plan;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGuardPlanResolverTest {

    @Test
    void keepsLastValidSnapshotAfterInvalidNewerUpdate() {
        MutableGuardPlanSource source = new MutableGuardPlanSource("dynamic", 100);
        try (DefaultGuardPlanResolver resolver = resolver(source)) {
            source.publish(snapshot(1, plan(10)));
            source.publish(snapshot(2, plan(0)));

            assertThat(resolver.resolve("draw").version()).isEqualTo(1L);
            assertThat(resolver.lastFailure("draw")).isPresent();
        }
    }

    @Test
    void rejectsNonMonotonicDynamicVersion() {
        MutableGuardPlanSource source = new MutableGuardPlanSource("dynamic", 100);
        try (DefaultGuardPlanResolver resolver = resolver(source)) {
            source.publish(snapshot(2, plan(10)));

            assertThatThrownBy(() -> source.publish(snapshot(1, plan(10))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monotonic");
            assertThat(resolver.resolve("draw").version()).isEqualTo(2L);
        }
    }

    @Test
    void usesStaticUntilDynamicPublishesAndNeverFallsBackAfterInvalidUpdate() {
        MutableGuardPlanSource properties = new MutableGuardPlanSource("properties", 0);
        MutableGuardPlanSource dynamic = new MutableGuardPlanSource("dynamic", 100);
        properties.publish(snapshot(0, plan(5)));

        try (DefaultGuardPlanResolver resolver = resolver(properties, dynamic)) {
            assertThat(resolver.resolve("draw").source()).isEqualTo("properties");
            dynamic.publish(snapshot(1, plan(10)));
            dynamic.publish(snapshot(2, plan(0)));

            assertThat(resolver.resolve("draw").source()).isEqualTo("dynamic");
            assertThat(resolver.resolve("draw").version()).isEqualTo(1L);
        }
    }

    @Test
    void rejectsAmbiguousSourcePriorities() {
        assertThatThrownBy(() -> resolver(
                new MutableGuardPlanSource("one", 100),
                new MutableGuardPlanSource("two", 100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority");
    }

    private static DefaultGuardPlanResolver resolver(GuardPlanSource... sources) {
        return new DefaultGuardPlanResolver(List.of(sources), new GuardPlanValidator());
    }

    private static GuardPlanSnapshot snapshot(long version, GuardPlan plan) {
        return new GuardPlanSnapshot("draw", version, Instant.EPOCH.plusSeconds(version), "dynamic", plan, "fp-" + version);
    }

    private static GuardPlan plan(long capacity) {
        return new GuardPlan(
                "draw",
                true,
                new KeyConfig(List.of("ARGUMENT"), List.of(), ""),
                new AdmissionConfig(
                        new AdmissionConfig.DenyListConfig(true),
                        new AdmissionConfig.AllowListConfig(true, AllowListMode.GATE),
                        new AdmissionConfig.PenaltyBoxConfig(true, 3, Duration.ofMinutes(1), Duration.ofMinutes(10)),
                        new AdmissionConfig.RateLimitConfig(
                                true,
                                AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                                capacity,
                                10,
                                Duration.ofSeconds(1),
                                1)),
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(
                                false,
                                TimeLimitMode.DISABLED,
                                TimeLimiterType.CALLER_THREAD,
                                Duration.ofSeconds(1),
                                true),
                        new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", "")),
                FailurePolicies.uniform(FailurePolicy.FAIL_CLOSED),
                ObservabilityConfig.defaults(),
                "state-v1");
    }

    private static final class MutableGuardPlanSource implements GuardPlanSource {

        private final String name;
        private final int priority;
        private final Map<String, GuardPlanSnapshot> snapshots = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<Consumer<GuardPlanSnapshot>> listeners = new CopyOnWriteArrayList<>();

        private MutableGuardPlanSource(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        private void publish(GuardPlanSnapshot snapshot) {
            snapshots.put(snapshot.ruleId(), snapshot);
            List<RuntimeException> failures = new ArrayList<>();
            listeners.forEach(listener -> {
                try {
                    listener.accept(snapshot);
                } catch (RuntimeException exception) {
                    failures.add(exception);
                }
            });
            if (!failures.isEmpty()) {
                throw failures.getFirst();
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public Optional<GuardPlanSnapshot> current(String ruleId) {
            return Optional.ofNullable(snapshots.get(ruleId));
        }

        @Override
        public AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }
}
