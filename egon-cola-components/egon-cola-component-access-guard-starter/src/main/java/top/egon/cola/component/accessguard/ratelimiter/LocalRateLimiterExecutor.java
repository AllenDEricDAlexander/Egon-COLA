package top.egon.cola.component.accessguard.ratelimiter;

import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class LocalRateLimiterExecutor implements RateLimiterExecutor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final LongSupplier nanoTimeSupplier;

    public LocalRateLimiterExecutor() {
        this(System::nanoTime);
    }

    public LocalRateLimiterExecutor(LongSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;
    }

    @Override
    public RateLimiterDecision tryAcquire(AccessGuardRule rule, AccessGuardContext context) {
        if (!rule.rateLimiterEnabled()) {
            return RateLimiterDecision.allow(Long.MAX_VALUE);
        }

        String bucketKey = rule.name() + ":" + context.accessKeyHash();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, ignored -> new Bucket(rule.permits(), intervalNanos(rule), nanoTimeSupplier.getAsLong()));
        return bucket.tryAcquire(nanoTimeSupplier.getAsLong());
    }

    private long intervalNanos(AccessGuardRule rule) {
        return rule.intervalUnit().toNanos(rule.interval());
    }

    private static class Bucket {

        private final long capacity;

        private final long intervalNanos;

        private long tokens;

        private long nextRefillNanos;

        Bucket(long capacity, long intervalNanos, long nowNanos) {
            this.capacity = Math.max(1L, capacity);
            this.intervalNanos = Math.max(1L, intervalNanos);
            this.tokens = this.capacity;
            this.nextRefillNanos = nowNanos + this.intervalNanos;
        }

        synchronized RateLimiterDecision tryAcquire(long nowNanos) {
            refillIfNeeded(nowNanos);
            if (tokens <= 0) {
                return RateLimiterDecision.reject("local rate limited");
            }
            tokens--;
            return RateLimiterDecision.allow(tokens);
        }

        private void refillIfNeeded(long nowNanos) {
            if (nowNanos < nextRefillNanos) {
                return;
            }
            long elapsedIntervals = ((nowNanos - nextRefillNanos) / intervalNanos) + 1;
            tokens = capacity;
            nextRefillNanos += TimeUnit.NANOSECONDS.toNanos(elapsedIntervals * intervalNanos);
        }
    }
}
