package top.egon.cola.component.accessguard.store.local;

import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public final class LocalRateLimitBackend implements RateLimitBackend {

    private final LongSupplier ticker;
    private final int maxEntries;
    private final long idleTtlNanos;
    private final ConcurrentHashMap<BucketKey, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger entryCount = new AtomicInteger();

    public LocalRateLimitBackend(LongSupplier ticker, int maxEntries, Duration idleTtl) {
        this.ticker = java.util.Objects.requireNonNull(ticker, "ticker");
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (idleTtl == null || idleTtl.isZero() || idleTtl.isNegative()) {
            throw new IllegalArgumentException("idleTtl must be positive");
        }
        this.maxEntries = maxEntries;
        this.idleTtlNanos = idleTtl.toNanos();
    }

    @Override
    public RateLimitDecision acquire(RateLimitRequest request) {
        long now = ticker.getAsLong();
        BucketKey key = new BucketKey(request.ruleId(), request.stateVersion(), request.keyHash());
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();
        buckets.compute(key, (ignored, existing) -> {
            if (existing == null) {
                reserve();
                existing = new Bucket(
                        request.capacity(), now, now,
                        request.capacity(), request.refillTokens(), request.refillPeriod().toNanos());
            } else if (!existing.matches(request)) {
                existing = new Bucket(
                        request.capacity(), now, now,
                        request.capacity(), request.refillTokens(), request.refillPeriod().toNanos());
            }
            Bucket refilled = existing.refill(now);
            if (refilled.tokens >= request.requestedTokens()) {
                long remaining = refilled.tokens - request.requestedTokens();
                decision.set(new RateLimitDecision(true, remaining, Duration.ZERO));
                return refilled.withTokens(remaining, now);
            }
            long missing = request.requestedTokens() - refilled.tokens;
            long periodsNeeded = divideCeiling(missing, request.refillTokens());
            long elapsedSinceBoundary = Math.max(0L, now - refilled.lastRefillNanos);
            long waitNanos = Math.max(0L,
                    Math.multiplyExact(periodsNeeded, request.refillPeriod().toNanos()) - elapsedSinceBoundary);
            decision.set(new RateLimitDecision(false, refilled.tokens, Duration.ofNanos(waitNanos)));
            return refilled.withLastAccess(now);
        });
        return decision.get();
    }

    @Override
    public int evictExpired() {
        long now = ticker.getAsLong();
        AtomicInteger removed = new AtomicInteger();
        buckets.forEach((key, bucket) -> {
            if (now - bucket.lastAccessNanos >= idleTtlNanos && buckets.remove(key, bucket)) {
                entryCount.decrementAndGet();
                removed.incrementAndGet();
            }
        });
        return removed.get();
    }

    @Override
    public int size() {
        return entryCount.get();
    }

    private void reserve() {
        while (true) {
            int current = entryCount.get();
            if (current >= maxEntries) {
                throw new StoreOperationException("rate-limit store capacity exceeded");
            }
            if (entryCount.compareAndSet(current, current + 1)) {
                return;
            }
        }
    }

    private static long divideCeiling(long value, long divisor) {
        return Math.addExact(value, divisor - 1L) / divisor;
    }

    private record BucketKey(String ruleId, String stateVersion, String keyHash) {
    }

    private record Bucket(
            long tokens,
            long lastRefillNanos,
            long lastAccessNanos,
            long capacity,
            long refillTokens,
            long refillPeriodNanos
    ) {

        private boolean matches(RateLimitRequest request) {
            return capacity == request.capacity()
                    && refillTokens == request.refillTokens()
                    && refillPeriodNanos == request.refillPeriod().toNanos();
        }

        private Bucket refill(long now) {
            long elapsed = Math.max(0L, now - lastRefillNanos);
            long periods = elapsed / refillPeriodNanos;
            if (periods == 0) {
                return withLastAccess(now);
            }
            long added;
            try {
                added = Math.multiplyExact(periods, refillTokens);
            } catch (ArithmeticException exception) {
                added = capacity;
            }
            long refilled = added >= capacity - tokens ? capacity : tokens + added;
            long boundary = Math.addExact(lastRefillNanos, Math.multiplyExact(periods, refillPeriodNanos));
            return new Bucket(refilled, boundary, now, capacity, refillTokens, refillPeriodNanos);
        }

        private Bucket withTokens(long value, long now) {
            return new Bucket(value, lastRefillNanos, now, capacity, refillTokens, refillPeriodNanos);
        }

        private Bucket withLastAccess(long now) {
            return new Bucket(tokens, lastRefillNanos, now, capacity, refillTokens, refillPeriodNanos);
        }
    }
}
