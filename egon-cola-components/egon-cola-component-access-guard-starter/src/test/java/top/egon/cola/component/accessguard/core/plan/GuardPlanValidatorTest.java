package top.egon.cola.component.accessguard.core.plan;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardPlanValidatorTest {

    private final GuardPlanValidator validator = new GuardPlanValidator();

    @Test
    void acceptsAllSupportedAlgorithmsWithValidValues() {
        for (AdmissionConfig.RateLimitAlgorithm algorithm : AdmissionConfig.RateLimitAlgorithm.values()) {
            assertThatCode(() -> validator.validate(snapshot(algorithm, 100, 1)))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsSlidingWindowRequestCostOtherThanOne() {
        assertThatThrownBy(() -> validator.validate(
                snapshot(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 100, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestedTokens=1");
    }

    @Test
    void rejectsOversizedSlidingWindow() {
        assertThatThrownBy(() -> validator.validate(
                snapshot(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 100_001, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100000");
    }

    private static GuardPlanSnapshot snapshot(
            AdmissionConfig.RateLimitAlgorithm algorithm,
            long capacity,
            long requestedTokens) {
        GuardPlan plan = new GuardPlan(
                "draw",
                true,
                new KeyConfig(java.util.List.of("GLOBAL"), java.util.List.of(), "secret"),
                new AdmissionConfig(
                        new AdmissionConfig.DenyListConfig(false),
                        new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                        new AdmissionConfig.PenaltyBoxConfig(
                                false, 1, Duration.ofSeconds(1), Duration.ofSeconds(1)),
                        new AdmissionConfig.RateLimitConfig(
                                true, algorithm, capacity, 1, Duration.ofSeconds(1), requestedTokens)),
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(
                                false, TimeLimitMode.DISABLED, TimeLimiterType.CALLER_THREAD,
                                Duration.ofSeconds(1), true),
                        new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", "")),
                FailurePolicies.uniform(FailurePolicy.FAIL_CLOSED),
                ObservabilityConfig.defaults(),
                "state-v1");
        return new GuardPlanSnapshot(
                "draw", 1L, Instant.EPOCH, "test", plan, "fingerprint");
    }
}
