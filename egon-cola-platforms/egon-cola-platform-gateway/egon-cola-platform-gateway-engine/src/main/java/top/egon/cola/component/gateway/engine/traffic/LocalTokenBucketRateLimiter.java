package top.egon.cola.component.gateway.engine.traffic;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class LocalTokenBucketRateLimiter {

    private final LongSupplier nanoTime;

    private final LongSupplier epochMillis;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>();

    public LocalTokenBucketRateLimiter(
            LongSupplier nanoTime,
            LongSupplier epochMillis) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.epochMillis = Objects.requireNonNull(epochMillis, "epochMillis");
    }

    public synchronized RateLimitDecision acquire(
            LocalTokenBucketPolicy policy,
            String keyHash,
            long permits) {
        Objects.requireNonNull(policy, "policy");
        if (keyHash == null || keyHash.isBlank() || permits < 1) {
            throw new IllegalArgumentException(
                    "keyHash and positive permits are required"
            );
        }
        long now = nanoTime.getAsLong();
        String stateKey = policy.stateKey(keyHash);
        evict(policy, now, stateKey);
        Bucket bucket = buckets.computeIfAbsent(
                stateKey,
                ignored -> new Bucket(policy.initialTokens(), now)
        );
        bucket.refill(policy, now);
        bucket.lastAccessNanos = now;
        boolean allowed = bucket.tokens >= permits;
        if (allowed) {
            bucket.tokens -= permits;
        }
        long missing = Math.max(0, permits - bucket.tokens);
        long periods = missing == 0
                ? 0
                : (missing + policy.refillTokens() - 1)
                / policy.refillTokens();
        long retryNanos = periods * policy.refillPeriod().toNanos();
        return new RateLimitDecision(
                allowed,
                bucket.tokens,
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        retryNanos
                ),
                epochMillis.getAsLong()
                        + java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        retryNanos
                ),
                false,
                false
        );
    }

    public synchronized int stateCount() {
        return buckets.size();
    }

    private void evict(
            LocalTokenBucketPolicy policy,
            long now,
            String requestedStateKey) {
        long earliest = now - policy.idleTtl().toNanos();
        buckets.entrySet().removeIf(
                entry -> entry.getValue().lastAccessNanos < earliest
        );
        while (!buckets.containsKey(requestedStateKey)
                && buckets.size() >= policy.maximumKeys()) {
            String oldest = buckets.entrySet().stream()
                    .min(Comparator.comparingLong(
                            entry -> entry.getValue().lastAccessNanos
                    ))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest == null) {
                return;
            }
            buckets.remove(oldest);
        }
    }

    private static final class Bucket {

        private long tokens;

        private long lastRefillNanos;

        private long lastAccessNanos;

        private Bucket(long tokens, long now) {
            this.tokens = tokens;
            lastRefillNanos = now;
            lastAccessNanos = now;
        }

        private void refill(LocalTokenBucketPolicy policy, long now) {
            long elapsed = Math.max(0, now - lastRefillNanos);
            long periods = elapsed / policy.refillPeriod().toNanos();
            if (periods == 0) {
                return;
            }
            long refill;
            try {
                refill = Math.multiplyExact(periods, policy.refillTokens());
            } catch (ArithmeticException overflow) {
                refill = policy.capacity();
            }
            tokens = Math.min(policy.capacity(), tokens + refill);
            lastRefillNanos += periods * policy.refillPeriod().toNanos();
        }
    }
}
