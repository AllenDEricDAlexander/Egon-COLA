package top.egon.cola.component.accessguard.policy.ratelimit;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPolicyTest {

    @Test
    void exposesTypedIdentityAndMapsFullConfigurationToBackendRequest() {
        AtomicReference<RateLimitRequest> captured = new AtomicReference<>();
        RateLimitPolicy policy = new RateLimitPolicy(request -> {
            captured.set(request);
            return new RateLimitDecision(false, 2, Duration.ofSeconds(3));
        });
        AdmissionConfig.RateLimitConfig config = new AdmissionConfig.RateLimitConfig(
                true, AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET,
                20, 7, Duration.ofSeconds(5), 3);

        PolicyResult result = policy.evaluate(
                GuardContext.forPolicy("draw", 4L, "state-v2", hash()),
                admission(config));

        assertThat(policy.type()).isEqualTo(GuardPolicyType.RATE_LIMIT);
        assertThat(result.allowed()).isFalse();
        assertThat(result.decision()).isEqualTo(GuardDecision.RATE_LIMITED);
        assertThat(result.retryAfter()).isEqualTo(Duration.ofSeconds(3));
        assertThat(result.remainingTokens()).isEqualTo(2);
        assertThat(captured).hasValueSatisfying(request -> {
            assertThat(request.ruleId()).isEqualTo("draw");
            assertThat(request.stateVersion()).isEqualTo("state-v2");
            assertThat(request.keyHash()).isEqualTo(hash());
            assertThat(request.algorithm()).isEqualTo(AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET);
            assertThat(request.capacity()).isEqualTo(20);
            assertThat(request.refillTokens()).isEqualTo(7);
            assertThat(request.refillPeriod()).isEqualTo(Duration.ofSeconds(5));
            assertThat(request.requestedTokens()).isEqualTo(3);
        });
    }

    @Test
    void disabledRateLimitDoesNotCallBackend() {
        RateLimitPolicy policy = new RateLimitPolicy(request -> {
            throw new AssertionError("disabled rate limit must not call the backend");
        });

        assertThat(policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                admission(new AdmissionConfig.RateLimitConfig(
                        false, AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10, 10, Duration.ofSeconds(1), 1))).decision())
                .isEqualTo(GuardDecision.PASS);
    }

    @Test
    void backendFailurePropagates() {
        RateLimitPolicy policy = new RateLimitPolicy(request -> {
            throw new StoreOperationException("RATE_LIMIT_UNAVAILABLE");
        });

        assertThatThrownBy(() -> policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                admission(new AdmissionConfig.RateLimitConfig(
                        true, AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10, 10, Duration.ofSeconds(1), 1))))
                .isInstanceOf(StoreOperationException.class)
                .hasMessageContaining("RATE_LIMIT_UNAVAILABLE");
    }

    private static AdmissionConfig admission(AdmissionConfig.RateLimitConfig rateLimit) {
        return new AdmissionConfig(
                new AdmissionConfig.DenyListConfig(false),
                new AdmissionConfig.AllowListConfig(false,
                        top.egon.cola.component.accessguard.policy.allow.AllowListMode.GATE),
                new AdmissionConfig.PenaltyBoxConfig(
                        false, 3, Duration.ofMinutes(1), Duration.ofMinutes(10)),
                rateLimit);
    }

    private static String hash() {
        return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
}
