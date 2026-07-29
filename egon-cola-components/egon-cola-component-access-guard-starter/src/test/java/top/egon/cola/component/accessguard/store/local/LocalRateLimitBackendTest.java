package top.egon.cola.component.accessguard.store.local;

import org.junit.jupiter.api.Test;
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

    private static RateLimitRequest request(
            String stateVersion,
            long capacity,
            long refill,
            Duration period,
            long requested
    ) {
        return new RateLimitRequest("draw", stateVersion, hash(), capacity, refill, period, requested);
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
