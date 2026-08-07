package top.egon.cola.component.gateway.contract.rule;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 作用于单个 Operation 的调用治理策略的类型化视图。
 *
 * <p>Engine 仍以 {@link GatewayRuntimePolicy} 的无类型配置运行，本类型为管理端和发布校验
 * 提供明确字段，并由 {@link ServiceCallPolicyCodec} 负责双向转换。它不新增另一套治理机制，
 * 而是把超时、重试、负载均衡、熔断和缓存统一表达为可校验的契约。
 *
 * @param timeout 调用总超时时间
 * @param retry 重试行为及幂等保护
 * @param loadBalance provider 实例选择策略
 * @param circuitBreaker 失败率熔断策略
 * @param cache 响应缓存策略
 */
public record ServiceCallPolicy(
        Duration timeout,
        RetryPolicy retry,
        LoadBalancePolicy loadBalance,
        CircuitBreakerPolicy circuitBreaker,
        CachePolicy cache
) {

    /** 未配置 TIMEOUT 策略时 Engine 使用的默认调用超时。 */
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
     * 检查策略与操作幂等性组合是否存在安全风险。
     *
     * @return 描述问题的中文信息；组合安全时返回空值
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
     * 调用失败重试策略。
     *
     * <p>字段名称和默认值与 Engine 的 RETRY 编译器保持一致；默认只重试幂等操作，避免超时的
     * 写请求被重复执行。
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

        /** 判断当前操作是否允许实际发起重试。 */
        public boolean appliesTo(boolean operationIsIdempotent) {
            if (!enabled || maxAttempts <= 1) {
                return false;
            }
            return operationIsIdempotent || !retryOnlyIdempotent;
        }
    }

    /**
     * provider 实例选择策略。
     *
     * <p>算法名称与 Engine 注册表一致；一致性哈希使用 {@code hashKey} 指定参与哈希的请求
     * 属性，并可优先选择同可用区实例。
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
     * 基于失败率的熔断策略。
     *
     * <p>默认值与 Engine 内置策略一致，使用该类型不会悄悄改变原有熔断行为。
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
                // 否则窗口会在积累到最小调用数前被丢弃，熔断器永远无法评估失败率。
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
     * 响应缓存策略。
     *
     * <p>缓存只适用于幂等且读型请求，{@link #cacheableRequestMethod(String)} 提供基础方法
     * 白名单；{@code varyHeaders} 用于把调用方差异纳入缓存键。
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

        /** 判断请求方法是否属于允许缓存的只读方法。 */
        public static boolean cacheableRequestMethod(String requestMethod) {
            return "GET".equalsIgnoreCase(requestMethod) || "HEAD".equalsIgnoreCase(requestMethod);
        }
    }

}
