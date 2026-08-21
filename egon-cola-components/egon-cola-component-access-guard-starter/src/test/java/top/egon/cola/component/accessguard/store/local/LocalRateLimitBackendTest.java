package top.egon.cola.component.accessguard.store.local;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalRateLimitBackendTest {

    @Test
    void refillsTokensProportionallyInsteadOfResettingWholeCapacity() {
        MutableTicker ticker = new MutableTicker();
        LocalRateLimitBackend backend = new LocalRateLimitBackend(ticker::read, 100, Duration.ofMinutes(10));

        assertThat(backend.acquire(request("state-v1", 10, 2, Duration.ofSeconds(1), 10)).allowed()).isTrue();
        ticker.advance(Duration.ofSeconds(1));
        RateLimitDecision decision = backend.acquire(request("state-v1", 10, 2, Duration.ofSeconds(1), 3));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remainingTokens()).isEqualTo(2L);
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void stateVersionStartsWithAnIndependentFullBucket() {
        MutableTicker ticker = new MutableTicker();
        LocalRateLimitBackend backend = new LocalRateLimitBackend(ticker::read, 100, Duration.ofMinutes(10));
        backend.acquire(request("state-v1", 1, 1, Duration.ofSeconds(1), 1));

        assertThat(backend.acquire(request("state-v2", 1, 1, Duration.ofSeconds(1), 1)).allowed()).isTrue();
    }

    @Test
    void boundedCapacityRejectsANewBucket() {
        LocalRateLimitBackend backend = new LocalRateLimitBackend(() -> 0L, 1, Duration.ofMinutes(10));
        backend.acquire(request("state-v1", 1, 1, Duration.ofSeconds(1), 1));

        assertThatThrownBy(() -> backend.acquire(new RateLimitRequest(
                "other", "state-v1", hash(), 1, 1, Duration.ofSeconds(1), 1)))
                .isInstanceOf(StoreOperationException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void idleBucketsAreEvictedWithoutSleeping() {
        MutableTicker ticker = new MutableTicker();
        LocalRateLimitBackend backend = new LocalRateLimitBackend(ticker::read, 10, Duration.ofMinutes(10));
        backend.acquire(request("state-v1", 1, 1, Duration.ofSeconds(1), 1));
        ticker.advance(Duration.ofMinutes(10));

        assertThat(backend.evictExpired()).isEqualTo(1);
        assertThat(backend.size()).isZero();
    }

    @Test
    void leakyBucketLeaksByFullPeriodsBeforeAdmittingMoreWater() {
        MutableTicker ticker = new MutableTicker();
        LocalRateLimitBackend backend = new LocalRateLimitBackend(
                ticker::read, 100, Duration.ofMinutes(10));

        assertThat(backend.acquire(request(
                AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET,
                "state-v1", 3, 1, Duration.ofSeconds(1), 3)).allowed()).isTrue();
        assertThat(backend.acquire(request(
                AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET,
                "state-v1", 3, 1, Duration.ofSeconds(1), 1)).allowed()).isFalse();

        ticker.advance(Duration.ofSeconds(1));
        assertThat(backend.acquire(request(
                AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET,
                "state-v1", 3, 1, Duration.ofSeconds(1), 1)).allowed()).isTrue();
    }

    @Test
    void slidingWindowExpiresTheOldestTimestampAtTheBoundary() {
        MutableTicker ticker = new MutableTicker();
        LocalRateLimitBackend backend = new LocalRateLimitBackend(
                ticker::read, 100, Duration.ofMinutes(10));
        RateLimitRequest request = request(
                AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW,
                "state-v1", 2, 1, Duration.ofSeconds(1), 1);

        assertThat(backend.acquire(request).allowed()).isTrue();
        assertThat(backend.acquire(request).allowed()).isTrue();
        assertThat(backend.acquire(request).allowed()).isFalse();
        ticker.advance(Duration.ofSeconds(1));
        assertThat(backend.acquire(request).allowed()).isTrue();
    }

    private static RateLimitRequest request(
            String stateVersion,
            long capacity,
            long refill,
            Duration period,
            long requested
    ) {
        return request(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                stateVersion, capacity, refill, period, requested);
    }

    private static RateLimitRequest request(
            AdmissionConfig.RateLimitAlgorithm algorithm,
            String stateVersion,
            long capacity,
            long refill,
            Duration period,
            long requested
    ) {
        return new RateLimitRequest(
                "draw", stateVersion, hash(), algorithm, capacity, refill, period, requested);
    }

    private static String hash() {
        return "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    }

    static final class MutableTicker {

        private final AtomicLong nanos = new AtomicLong();

        long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
