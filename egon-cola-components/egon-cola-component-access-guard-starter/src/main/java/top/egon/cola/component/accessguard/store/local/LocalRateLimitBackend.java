package top.egon.cola.component.accessguard.store.local;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitAlgorithmStrategy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitAlgorithmStrategyFactory;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

public final class LocalRateLimitBackend implements RateLimitBackend {

    private final LongSupplier ticker;
    private final int maxEntries;
    private final long idleTtlNanos;
    private final AtomicInteger entryCount = new AtomicInteger();
    private final RateLimitAlgorithmStrategyFactory strategies;

    public LocalRateLimitBackend(
            LongSupplier ticker,
            int maxEntries,
            Duration idleTtl) {
        this.ticker = java.util.Objects.requireNonNull(ticker, "ticker");
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (idleTtl == null || idleTtl.isZero() || idleTtl.isNegative()) {
            throw new IllegalArgumentException("idleTtl must be positive");
        }
        this.maxEntries = maxEntries;
        this.idleTtlNanos = idleTtl.toNanos();
        this.strategies = new RateLimitAlgorithmStrategyFactory(java.util.List.of(
                new TokenBucketStrategy(),
                new LeakyBucketStrategy(),
                new SlidingWindowStrategy()));
    }

    @Override
    public RateLimitDecision acquire(RateLimitRequest request) {
        return strategies.acquire(request);
    }

    @Override
    public int evictExpired() {
        return strategies.evictExpired();
    }

    @Override
    public int size() {
        return strategies.size();
    }

    private void reserve() {
        while (true) {
            int current = entryCount.get();
            if (current >= maxEntries) {
                throw new StoreOperationException(
                        "rate-limit store capacity exceeded");
            }
            if (entryCount.compareAndSet(current, current + 1)) {
                return;
            }
        }
    }

    private void release() {
        entryCount.decrementAndGet();
    }

    private abstract class LocalStrategy implements RateLimitAlgorithmStrategy {

        protected void requireAlgorithm(RateLimitRequest request) {
            if (request.algorithm() != algorithm()) {
                throw new IllegalArgumentException(
                        "rate-limit algorithm mismatch: expected "
                                + algorithm() + " but was " + request.algorithm());
            }
        }
    }

    private final class TokenBucketStrategy extends LocalStrategy {

        private final ConcurrentHashMap<BucketKey, TokenBucket> buckets =
                new ConcurrentHashMap<>();

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET;
        }

        @Override
        public RateLimitDecision acquire(RateLimitRequest request) {
            requireAlgorithm(request);
            long now = ticker.getAsLong();
            BucketKey key = BucketKey.from(request);
            AtomicReference<RateLimitDecision> result = new AtomicReference<>();
            buckets.compute(key, (ignored, current) -> {
                TokenBucket bucket = current;
                if (bucket == null) {
                    reserve();
                    bucket = TokenBucket.initial(request, now);
                } else if (!bucket.matches(request)) {
                    bucket = TokenBucket.initial(request, now);
                }
                TokenBucket refilled = bucket.refill(now);
                if (refilled.tokens() >= request.requestedTokens()) {
                    long remaining = refilled.tokens() - request.requestedTokens();
                    result.set(new RateLimitDecision(true, remaining, Duration.ZERO));
                    return refilled.withTokens(remaining, now);
                }
                long missing = request.requestedTokens() - refilled.tokens();
                long periodsNeeded = divideCeiling(missing, request.refillTokens());
                long elapsed = Math.max(0L, now - refilled.lastRefillNanos());
                long wait = safeWait(periodsNeeded, request.refillPeriod().toNanos(), elapsed);
                result.set(new RateLimitDecision(
                        false, refilled.tokens(), Duration.ofNanos(wait)));
                return refilled.withLastAccess(now);
            });
            return result.get();
        }

        @Override
        public int evictExpired() {
            return evict(buckets, (key, value) -> value.lastAccessNanos());
        }

        @Override
        public int size() {
            return buckets.size();
        }
    }

    private final class LeakyBucketStrategy extends LocalStrategy {

        private final ConcurrentHashMap<BucketKey, LeakyBucket> buckets =
                new ConcurrentHashMap<>();

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET;
        }

        @Override
        public RateLimitDecision acquire(RateLimitRequest request) {
            requireAlgorithm(request);
            long now = ticker.getAsLong();
            BucketKey key = BucketKey.from(request);
            AtomicReference<RateLimitDecision> result = new AtomicReference<>();
            buckets.compute(key, (ignored, current) -> {
                LeakyBucket bucket = current;
                if (bucket == null) {
                    reserve();
                    bucket = LeakyBucket.initial(request, now);
                } else if (!bucket.matches(request)) {
                    bucket = LeakyBucket.initial(request, now);
                }
                LeakyBucket leaked = bucket.leak(now);
                if (leaked.level() + request.requestedTokens() <= leaked.capacity()) {
                    long level = leaked.level() + request.requestedTokens();
                    result.set(new RateLimitDecision(
                            true, leaked.capacity() - level, Duration.ZERO));
                    return leaked.withLevel(level, now);
                }
                long missing = leaked.level() + request.requestedTokens()
                        - leaked.capacity();
                long periodsNeeded = divideCeiling(missing, request.refillTokens());
                long elapsed = Math.max(0L, now - leaked.lastLeakNanos());
                long wait = safeWait(periodsNeeded, request.refillPeriod().toNanos(), elapsed);
                result.set(new RateLimitDecision(
                        false, leaked.capacity() - leaked.level(), Duration.ofNanos(wait)));
                return leaked.withLastAccess(now);
            });
            return result.get();
        }

        @Override
        public int evictExpired() {
            return evict(buckets, (key, value) -> value.lastAccessNanos());
        }

        @Override
        public int size() {
            return buckets.size();
        }
    }

    private final class SlidingWindowStrategy extends LocalStrategy {

        private final ConcurrentHashMap<BucketKey, SlidingWindow> windows =
                new ConcurrentHashMap<>();

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW;
        }

        @Override
        public RateLimitDecision acquire(RateLimitRequest request) {
            requireAlgorithm(request);
            long now = ticker.getAsLong();
            BucketKey key = BucketKey.from(request);
            AtomicReference<RateLimitDecision> result = new AtomicReference<>();
            windows.compute(key, (ignored, current) -> {
                SlidingWindow window = current;
                if (window == null) {
                    reserve();
                    window = SlidingWindow.initial(request, now);
                } else if (!window.matches(request)) {
                    window = SlidingWindow.initial(request, now);
                }
                SlidingWindow pruned = window.prune(now);
                if (pruned.timestamps().size() < pruned.capacity()) {
                    ArrayDeque<Long> timestamps = new ArrayDeque<>(pruned.timestamps());
                    timestamps.addLast(now);
                    result.set(new RateLimitDecision(
                            true, pruned.capacity() - timestamps.size(), Duration.ZERO));
                    return pruned.withTimestamps(timestamps, now);
                }
                long oldest = pruned.timestamps().peekFirst();
                long wait = Math.max(0L, safeSubtract(
                        safeAdd(oldest, pruned.windowNanos()), now));
                result.set(new RateLimitDecision(false, 0, Duration.ofNanos(wait)));
                return pruned.withLastAccess(now);
            });
            return result.get();
        }

        @Override
        public int evictExpired() {
            return evict(windows, (key, value) -> value.lastAccessNanos());
        }

        @Override
        public int size() {
            return windows.size();
        }
    }

    private <T> int evict(
            ConcurrentHashMap<BucketKey, T> states,
            BiFunction<BucketKey, T, Long> lastAccess) {
        long now = ticker.getAsLong();
        AtomicInteger removed = new AtomicInteger();
        states.forEach((key, state) -> {
            if (now - lastAccess.apply(key, state) >= idleTtlNanos
                    && states.remove(key, state)) {
                release();
                removed.incrementAndGet();
            }
        });
        return removed.get();
    }

    private static long divideCeiling(long value, long divisor) {
        if (value <= 0) {
            return 0;
        }
        return value > Long.MAX_VALUE - divisor + 1
                ? Long.MAX_VALUE / divisor
                : (value + divisor - 1L) / divisor;
    }

    private static long safeWait(long periods, long periodNanos, long elapsed) {
        return Math.max(0L, safeSubtract(safeMultiply(periods, periodNanos), elapsed));
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0 || right <= 0) {
            return 0;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long safeAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long safeSubtract(long left, long right) {
        return left < right ? 0 : left - right;
    }

    private static long windowBoundary(long now, long windowNanos) {
        return now < Long.MIN_VALUE + windowNanos
                ? Long.MIN_VALUE
                : now - windowNanos;
    }

    private record BucketKey(String ruleId, String stateVersion, String keyHash) {

        private static BucketKey from(RateLimitRequest request) {
            return new BucketKey(
                    request.ruleId(), request.stateVersion(), request.keyHash());
        }
    }

    private record TokenBucket(
            long tokens,
            long lastRefillNanos,
            long lastAccessNanos,
            long capacity,
            long refillTokens,
            long refillPeriodNanos) {

        private static TokenBucket initial(RateLimitRequest request, long now) {
            return new TokenBucket(
                    request.capacity(), now, now, request.capacity(),
                    request.refillTokens(), request.refillPeriod().toNanos());
        }

        private boolean matches(RateLimitRequest request) {
            return capacity == request.capacity()
                    && refillTokens == request.refillTokens()
                    && refillPeriodNanos == request.refillPeriod().toNanos();
        }

        private TokenBucket refill(long now) {
            long elapsed = Math.max(0L, now - lastRefillNanos);
            long periods = elapsed / refillPeriodNanos;
            if (periods == 0) {
                return withLastAccess(now);
            }
            long added = safeMultiply(periods, refillTokens);
            long refilled = Math.min(capacity, safeAdd(tokens, added));
            long boundary = safeAdd(
                    lastRefillNanos, safeMultiply(periods, refillPeriodNanos));
            return new TokenBucket(
                    refilled, boundary, now, capacity, refillTokens, refillPeriodNanos);
        }

        private TokenBucket withTokens(long value, long now) {
            return new TokenBucket(
                    value, lastRefillNanos, now, capacity, refillTokens, refillPeriodNanos);
        }

        private TokenBucket withLastAccess(long now) {
            return new TokenBucket(
                    tokens, lastRefillNanos, now, capacity, refillTokens, refillPeriodNanos);
        }
    }

    private record LeakyBucket(
            long level,
            long lastLeakNanos,
            long lastAccessNanos,
            long capacity,
            long leakTokens,
            long leakPeriodNanos) {

        private static LeakyBucket initial(RateLimitRequest request, long now) {
            return new LeakyBucket(
                    0, now, now, request.capacity(), request.refillTokens(),
                    request.refillPeriod().toNanos());
        }

        private boolean matches(RateLimitRequest request) {
            return capacity == request.capacity()
                    && leakTokens == request.refillTokens()
                    && leakPeriodNanos == request.refillPeriod().toNanos();
        }

        private LeakyBucket leak(long now) {
            long elapsed = Math.max(0L, now - lastLeakNanos);
            long periods = elapsed / leakPeriodNanos;
            if (periods == 0) {
                return withLastAccess(now);
            }
            long leaked = safeMultiply(periods, leakTokens);
            long remaining = leaked >= level ? 0 : level - leaked;
            long boundary = safeAdd(
                    lastLeakNanos, safeMultiply(periods, leakPeriodNanos));
            return new LeakyBucket(
                    remaining, boundary, now, capacity, leakTokens, leakPeriodNanos);
        }

        private LeakyBucket withLevel(long value, long now) {
            return new LeakyBucket(
                    value, lastLeakNanos, now, capacity, leakTokens, leakPeriodNanos);
        }

        private LeakyBucket withLastAccess(long now) {
            return new LeakyBucket(
                    level, lastLeakNanos, now, capacity, leakTokens, leakPeriodNanos);
        }
    }

    private record SlidingWindow(
            ArrayDeque<Long> timestamps,
            long lastAccessNanos,
            long capacity,
            long windowNanos) {

        private static SlidingWindow initial(RateLimitRequest request, long now) {
            return new SlidingWindow(
                    new ArrayDeque<>(), now, request.capacity(),
                    request.refillPeriod().toNanos());
        }

        private boolean matches(RateLimitRequest request) {
            return capacity == request.capacity()
                    && windowNanos == request.refillPeriod().toNanos();
        }

        private SlidingWindow prune(long now) {
            ArrayDeque<Long> copy = new ArrayDeque<>(timestamps);
            long boundary = windowBoundary(now, windowNanos);
            while (!copy.isEmpty() && copy.peekFirst() <= boundary) {
                copy.removeFirst();
            }
            return new SlidingWindow(copy, now, capacity, windowNanos);
        }

        private SlidingWindow withTimestamps(ArrayDeque<Long> value, long now) {
            return new SlidingWindow(value, now, capacity, windowNanos);
        }

        private SlidingWindow withLastAccess(long now) {
            return new SlidingWindow(
                    new ArrayDeque<>(timestamps), now, capacity, windowNanos);
        }
    }
}
