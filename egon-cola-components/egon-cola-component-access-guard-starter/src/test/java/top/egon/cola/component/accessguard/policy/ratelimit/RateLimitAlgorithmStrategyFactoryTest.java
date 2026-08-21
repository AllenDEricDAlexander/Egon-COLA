package top.egon.cola.component.accessguard.policy.ratelimit;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitAlgorithmStrategyFactoryTest {

    private static final String HASH = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    @Test
    void dispatchesEachSupportedAlgorithmToItsStrategy() {
        RateLimitAlgorithmStrategy token = strategy(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET, 1);
        RateLimitAlgorithmStrategy leaky = strategy(AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET, 2);
        RateLimitAlgorithmStrategy sliding = strategy(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 3);
        RateLimitAlgorithmStrategyFactory factory = new RateLimitAlgorithmStrategyFactory(
                List.of(token, leaky, sliding));

        assertThat(factory.acquire(request(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET)).remainingTokens())
                .isEqualTo(1);
        assertThat(factory.acquire(request(AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET)).remainingTokens())
                .isEqualTo(2);
        assertThat(factory.acquire(request(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW)).remainingTokens())
                .isEqualTo(3);
    }

    @Test
    void rejectsDuplicateAlgorithmMappings() {
        RateLimitAlgorithmStrategy token = strategy(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET, 1);

        assertThatThrownBy(() -> new RateLimitAlgorithmStrategyFactory(List.of(
                token, token,
                strategy(AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET, 2),
                strategy(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 3))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void rejectsIncompleteAlgorithmMappings() {
        assertThatThrownBy(() -> new RateLimitAlgorithmStrategyFactory(List.of(
                strategy(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coverage");
    }

    private static RateLimitAlgorithmStrategy strategy(
            AdmissionConfig.RateLimitAlgorithm algorithm,
            long remaining) {
        return new RateLimitAlgorithmStrategy() {
            @Override
            public AdmissionConfig.RateLimitAlgorithm algorithm() {
                return algorithm;
            }

            @Override
            public RateLimitDecision acquire(RateLimitRequest request) {
                return new RateLimitDecision(true, remaining, Duration.ZERO);
            }
        };
    }

    private static RateLimitRequest request(AdmissionConfig.RateLimitAlgorithm algorithm) {
        return new RateLimitRequest(
                "draw", "state-v1", HASH, algorithm, 10, 1,
                Duration.ofSeconds(1), 1);
    }
}
