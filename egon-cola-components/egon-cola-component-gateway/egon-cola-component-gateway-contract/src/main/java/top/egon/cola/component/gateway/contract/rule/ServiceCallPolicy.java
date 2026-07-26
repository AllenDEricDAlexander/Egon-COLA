package top.egon.cola.component.gateway.contract.rule;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Typed view over the call-governance policies that apply to one operation.
 *
 * <p>The gateway already governs calls through {@link GatewayRuntimePolicy} records whose
 * {@code configuration} is an untyped {@code Map<String, Object>}. That is workable for the
 * engine, which knows the keys it wants, but it leaves the management surface with nothing to
 * bind a form to and no way to validate a value before publishing it. This record is the typed
 * projection of those policies; {@link ServiceCallPolicyCodec} converts in both directions.
 *
 * <p>It deliberately introduces no new governance mechanism. Every field except
 * {@link #cache()} maps onto a policy type the engine already compiles — {@code TIMEOUT},
 * {@code RETRY}, {@code CIRCUIT_BREAKER} and the provider-side load-balance policy — using the
 * key names those compilers already read.
 *
 * @param timeout        overall call deadline
 * @param retry          retry behaviour, including the idempotency guard
 * @param loadBalance    provider selection strategy
 * @param circuitBreaker failure-rate tripping
 * @param cache          response caching; the one genuinely new capability here
 */
public record ServiceCallPolicy(
        Duration timeout,
        RetryPolicy retry,
        LoadBalancePolicy loadBalance,
        CircuitBreakerPolicy circuitBreaker,
        CachePolicy cache
) {

    /** Matches the engine's fallback when no TIMEOUT policy is present. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

    private static final ServiceCallPolicy DEFAULTS = new ServiceCallPolicy(
            DEFAULT_TIMEOUT,
            RetryPolicy.defaults(),
            LoadBalancePolicy.defaults(),
            CircuitBreakerPolicy.defaults(),
            CachePolicy.disabled());

    public ServiceCallPolicy {
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive but was " + timeout);
        }
        retry = retry == null ? RetryPolicy.defaults() : retry;
        loadBalance = loadBalance == null ? LoadBalancePolicy.defaults() : loadBalance;
        circuitBreaker = circuitBreaker == null ? CircuitBreakerPolicy.defaults() : circuitBreaker;
        cache = cache == null ? CachePolicy.disabled() : cache;
    }

    public static ServiceCallPolicy defaults() {
        return DEFAULTS;
    }

    /**
     * Whether this policy is safe to apply to an operation with the given idempotency.
     *
     * <p>Returns a description of the problem, or empty when the combination is sound. This is
     * surfaced in the management UI rather than enforced here, because the enforcement point
     * is {@link RetryPolicy#appliesTo(boolean)} at call time.
     */
    public java.util.Optional<String> unsafeReason(boolean operationIsIdempotent) {
        if (retry.enabled() && retry.maxAttempts() > 1
                && !operationIsIdempotent && !retry.retryOnlyIdempotent()) {
            return java.util.Optional.of(
                    "retry is enabled on a non-idempotent operation with retryOnlyIdempotent=false, "
                            + "so a failed call may be replayed and duplicate its side effect");
        }
        if (cache.enabled() && !operationIsIdempotent) {
            return java.util.Optional.of(
                    "response caching is enabled on a non-idempotent operation");
        }
        return java.util.Optional.empty();
    }

    /**
     * Retry behaviour.
     *
     * <p>Field names and defaults mirror the keys the engine's retry compiler already reads, so
     * a policy authored through this type and one hand-written before it behave identically.
     *
     * @param retryOnlyIdempotent when true — the default — a non-idempotent operation is never
     *                            retried regardless of the other settings. This is the guard
     *                            that stops the gateway turning one timed-out payment into two.
     */
    public record RetryPolicy(
            boolean enabled,
            int maxAttempts,
            Duration initialBackoff,
            Duration maximumBackoff,
            double multiplier,
            Duration minimumAttemptBudget,
            Set<Integer> retryableHttpStatuses,
            Set<String> retryableRpcStatuses,
            boolean retryOnlyIdempotent
    ) {

        public static final int MAX_ATTEMPTS_LIMIT = 5;

        private static final RetryPolicy DEFAULTS = new RetryPolicy(
                true, 2, Duration.ofMillis(10), Duration.ofMillis(100), 2.0d,
                Duration.ofMillis(20), Set.of(502, 503, 504),
                Set.of("UNAVAILABLE", "RESOURCE_EXHAUSTED", "ABORTED"), true);

        public RetryPolicy {
            if (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS_LIMIT) {
                throw new IllegalArgumentException(
                        "maxAttempts must be between 1 and " + MAX_ATTEMPTS_LIMIT
                                + " but was " + maxAttempts);
            }
            if (multiplier < 1.0d) {
                throw new IllegalArgumentException(
                        "multiplier must be at least 1.0 but was " + multiplier);
            }
            initialBackoff = PolicyDurations.requirePositive(initialBackoff, "initialBackoff");
            maximumBackoff = PolicyDurations.requirePositive(maximumBackoff, "maximumBackoff");
            if (maximumBackoff.compareTo(initialBackoff) < 0) {
                throw new IllegalArgumentException(
                        "maximumBackoff must be at least initialBackoff");
            }
            minimumAttemptBudget = PolicyDurations.requirePositive(minimumAttemptBudget, "minimumAttemptBudget");
            retryableHttpStatuses = Set.copyOf(Objects.requireNonNull(
                    retryableHttpStatuses, "retryableHttpStatuses"));
            retryableRpcStatuses = Set.copyOf(Objects.requireNonNull(
                    retryableRpcStatuses, "retryableRpcStatuses"));
        }

        public static RetryPolicy defaults() {
            return DEFAULTS;
        }

        public static RetryPolicy disabled() {
            return new RetryPolicy(false, 1, Duration.ofMillis(10), Duration.ofMillis(100), 2.0d,
                    Duration.ofMillis(20), Set.of(), Set.of(), true);
        }

        /** Whether retries may actually be attempted for an operation with this idempotency. */
        public boolean appliesTo(boolean operationIsIdempotent) {
            if (!enabled || maxAttempts <= 1) {
                return false;
            }
            return operationIsIdempotent || !retryOnlyIdempotent;
        }
    }

    /**
     * Provider selection.
     *
     * @param strategy       algorithm; names match the engine's load balancer registry
     * @param hashKey        request attribute hashed by {@code CONSISTENT_HASH}; ignored otherwise
     * @param preferSameZone try same-zone instances first, falling back across zones when none
     *                       are available rather than failing the call
     */
    public record LoadBalancePolicy(
            LoadBalanceStrategy strategy,
            String hashKey,
            boolean preferSameZone
    ) {

        private static final LoadBalancePolicy DEFAULTS =
                new LoadBalancePolicy(LoadBalanceStrategy.SMOOTH_WEIGHTED_ROUND_ROBIN, "", true);

        public LoadBalancePolicy {
            strategy = strategy == null ? LoadBalanceStrategy.SMOOTH_WEIGHTED_ROUND_ROBIN : strategy;
            hashKey = hashKey == null ? "" : hashKey.trim();
            if (strategy == LoadBalanceStrategy.CONSISTENT_HASH && hashKey.isEmpty()) {
                throw new IllegalArgumentException(
                        "CONSISTENT_HASH requires a hashKey, otherwise every request hashes alike");
            }
        }

        public static LoadBalancePolicy defaults() {
            return DEFAULTS;
        }
    }

    /**
     * Circuit breaking.
     *
     * <p>Defaults match the engine's built-in circuit policy so adopting this type does not
     * silently change tripping behaviour.
     */
    public record CircuitBreakerPolicy(
            boolean enabled,
            float failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            Duration openDuration,
            int halfOpenPermits
    ) {

        private static final CircuitBreakerPolicy DEFAULTS = new CircuitBreakerPolicy(
                false, 50f, 20, 10, Duration.ofSeconds(30), 3);

        public CircuitBreakerPolicy {
            if (failureRateThreshold <= 0f || failureRateThreshold > 100f) {
                throw new IllegalArgumentException(
                        "failureRateThreshold must be a percentage in (0, 100] but was "
                                + failureRateThreshold);
            }
            if (slidingWindowSize < 1) {
                throw new IllegalArgumentException("slidingWindowSize must be positive");
            }
            if (minimumNumberOfCalls < 1) {
                throw new IllegalArgumentException("minimumNumberOfCalls must be positive");
            }
            if (minimumNumberOfCalls > slidingWindowSize) {
                // Otherwise the breaker can never evaluate: the window is discarded before
                // enough calls accumulate to reach the minimum.
                throw new IllegalArgumentException(
                        "minimumNumberOfCalls must not exceed slidingWindowSize");
            }
            openDuration = PolicyDurations.requirePositive(openDuration, "openDuration");
            if (halfOpenPermits < 1) {
                throw new IllegalArgumentException("halfOpenPermits must be positive");
            }
        }

        public static CircuitBreakerPolicy defaults() {
            return DEFAULTS;
        }
    }

    /**
     * Response caching.
     *
     * <p>The only capability here the engine does not already have. Caching is unsafe for
     * anything that mutates state, so enabling it is gated on the operation being idempotent
     * and read-shaped; see {@link #cacheableRequestMethod(String)}.
     *
     * @param varyHeaders request headers that participate in the cache key, so a response
     *                    tailored to one caller is not served to another
     */
    public record CachePolicy(
            boolean enabled,
            Duration ttl,
            String keyExpression,
            Set<String> varyHeaders
    ) {

        public static final Duration MAX_TTL = Duration.ofHours(24);

        private static final CachePolicy DISABLED =
                new CachePolicy(false, Duration.ofSeconds(30), "", Set.of());

        public CachePolicy {
            ttl = ttl == null ? Duration.ofSeconds(30) : ttl;
            if (enabled) {
                if (ttl.isNegative() || ttl.isZero()) {
                    throw new IllegalArgumentException("cache ttl must be positive when enabled");
                }
                if (ttl.compareTo(MAX_TTL) > 0) {
                    throw new IllegalArgumentException("cache ttl must not exceed " + MAX_TTL);
                }
            }
            keyExpression = keyExpression == null ? "" : keyExpression.trim();
            varyHeaders = Objects.requireNonNull(varyHeaders, "varyHeaders").stream()
                    .map(header -> header.toLowerCase(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        public static CachePolicy disabled() {
            return DISABLED;
        }

        /**
         * Whether a request method is eligible for caching at all.
         *
         * <p>Restricted to the read-only methods. Caching a POST would let one caller's write
         * be answered from another's response.
         */
        public static boolean cacheableRequestMethod(String requestMethod) {
            return "GET".equalsIgnoreCase(requestMethod) || "HEAD".equalsIgnoreCase(requestMethod);
        }
    }

}
