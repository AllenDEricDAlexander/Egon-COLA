package top.egon.cola.component.gateway.contract.rule;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在 {@link ServiceCallPolicy} 类型化策略与 Engine 编译的 {@link GatewayRuntimePolicy} 之间
 * 进行投影转换。
 *
 * <p>编码时只覆盖本类型拥有的键，未知配置会保留；解码时对每个字段使用 Engine 默认值兜底，
 * 保证一条格式异常的已发布规则不会导致整份规则不可读。
 */
public final class ServiceCallPolicyCodec {

    public static final String TYPE_TIMEOUT = "TIMEOUT";
    public static final String TYPE_RETRY = "RETRY";
    public static final String TYPE_CIRCUIT_BREAKER = "CIRCUIT_BREAKER";
    public static final String TYPE_CACHE = "CACHE";
    public static final String TYPE_LOAD_BALANCE = "LOAD_BALANCE";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_TIMEOUT = "timeout";
    public static final String KEY_MAX_ATTEMPTS = "maxAttempts";
    public static final String KEY_INITIAL_BACKOFF = "initialBackoff";
    public static final String KEY_MAXIMUM_BACKOFF = "maximumBackoff";
    public static final String KEY_MULTIPLIER = "multiplier";
    public static final String KEY_MINIMUM_ATTEMPT_BUDGET = "minimumAttemptBudget";
    public static final String KEY_RETRYABLE_HTTP_STATUSES = "retryableHttpStatuses";
    public static final String KEY_RETRYABLE_RPC_STATUSES = "retryableRpcStatuses";
    public static final String KEY_RETRY_ONLY_IDEMPOTENT = "retryOnlyIdempotent";
    public static final String KEY_FAILURE_RATE_THRESHOLD = "failureRateThreshold";
    public static final String KEY_SLIDING_WINDOW_SIZE = "slidingWindowSize";
    public static final String KEY_MINIMUM_NUMBER_OF_CALLS = "minimumNumberOfCalls";
    public static final String KEY_OPEN_DURATION = "openDuration";
    public static final String KEY_HALF_OPEN_PERMITS = "halfOpenPermits";
    public static final String KEY_TTL = "ttl";
    public static final String KEY_KEY_EXPRESSION = "keyExpression";
    public static final String KEY_VARY_HEADERS = "varyHeaders";
    public static final String KEY_STRATEGY = "strategy";
    public static final String KEY_HASH_KEY = "hashKey";
    public static final String KEY_PREFER_SAME_ZONE = "preferSameZone";

    private ServiceCallPolicyCodec() {
    }

    /** 从 Operation 绑定的运行时策略构建类型化视图。 */
    public static ServiceCallPolicy decode(Collection<GatewayRuntimePolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            return ServiceCallPolicy.defaults();
        }
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (GatewayRuntimePolicy policy : policies) {
            if (policy != null) {
                byType.putIfAbsent(policy.type().toUpperCase(Locale.ROOT), policy.configuration());
            }
        }
        return new ServiceCallPolicy(
                decodeTimeout(byType.get(TYPE_TIMEOUT)),
                decodeRetry(byType.get(TYPE_RETRY)),
                decodeLoadBalance(byType.get(TYPE_LOAD_BALANCE)),
                decodeCircuitBreaker(byType.get(TYPE_CIRCUIT_BREAKER)),
                decodeCache(byType.get(TYPE_CACHE)));
    }

    /**
     * 将类型化视图编码回运行时策略，并保留本类型未建模的既有配置。
     *
     * @param policy 编辑后的类型化策略
     * @param existing 当前已绑定的策略，按类型匹配，可以为空
     * @param policyId 为此前不存在的策略类型生成 ID
     * @return 按稳定顺序排列的各类型策略
     */
    public static List<GatewayRuntimePolicy> encode(
            ServiceCallPolicy policy,
            Collection<GatewayRuntimePolicy> existing,
            java.util.function.Function<String, String> policyId) {
        Map<String, GatewayRuntimePolicy> current = new LinkedHashMap<>();
        if (existing != null) {
            existing.forEach(item -> {
                if (item != null) {
                    current.putIfAbsent(item.type().toUpperCase(Locale.ROOT), item);
                }
            });
        }
        List<GatewayRuntimePolicy> encoded = new ArrayList<>();
        encoded.add(merge(current, policyId, TYPE_TIMEOUT, encodeTimeout(policy.timeout())));
        encoded.add(merge(current, policyId, TYPE_RETRY, encodeRetry(policy.retry())));
        encoded.add(merge(current, policyId, TYPE_LOAD_BALANCE, encodeLoadBalance(policy.loadBalance())));
        encoded.add(merge(current, policyId, TYPE_CIRCUIT_BREAKER,
                encodeCircuitBreaker(policy.circuitBreaker())));
        encoded.add(merge(current, policyId, TYPE_CACHE, encodeCache(policy.cache())));
        return List.copyOf(encoded);
    }

    private static GatewayRuntimePolicy merge(
            Map<String, GatewayRuntimePolicy> current,
            java.util.function.Function<String, String> policyId,
            String type,
            Map<String, Object> owned) {
        GatewayRuntimePolicy previous = current.get(type);
        // 从已存配置开始，确保本类型未建模的键在编辑后仍然保留。
        Map<String, Object> configuration = new LinkedHashMap<>();
        if (previous != null) {
            configuration.putAll(previous.configuration());
        }
        configuration.putAll(owned);
        return new GatewayRuntimePolicy(
                previous != null ? previous.policyId() : policyId.apply(type),
                type,
                previous != null ? previous.scope() : "OPERATION",
                configuration);
    }

    private static Duration decodeTimeout(Map<String, Object> config) {
        if (config == null) {
            return ServiceCallPolicy.DEFAULT_TIMEOUT;
        }
        return duration(config, KEY_TIMEOUT, ServiceCallPolicy.DEFAULT_TIMEOUT);
    }

    private static Map<String, Object> encodeTimeout(Duration timeout) {
        return Map.of(KEY_TIMEOUT, timeout.toString());
    }

    private static ServiceCallPolicy.RetryPolicy decodeRetry(Map<String, Object> config) {
        ServiceCallPolicy.RetryPolicy defaults = ServiceCallPolicy.RetryPolicy.defaults();
        if (config == null) {
            return defaults;
        }
        boolean enabled = bool(config, KEY_ENABLED, defaults.enabled());
        int maxAttempts = clamp(integer(config, KEY_MAX_ATTEMPTS, defaults.maxAttempts()),
                1, ServiceCallPolicy.RetryPolicy.MAX_ATTEMPTS_LIMIT, defaults.maxAttempts());
        Duration initialBackoff = duration(config, KEY_INITIAL_BACKOFF, defaults.initialBackoff());
        Duration maximumBackoff = duration(config, KEY_MAXIMUM_BACKOFF, defaults.maximumBackoff());
        if (maximumBackoff.compareTo(initialBackoff) < 0) {
            maximumBackoff = initialBackoff;
        }
        double multiplier = Math.max(1.0d, doubleValue(config, KEY_MULTIPLIER, defaults.multiplier()));
        return new ServiceCallPolicy.RetryPolicy(
                enabled,
                maxAttempts,
                initialBackoff,
                maximumBackoff,
                multiplier,
                duration(config, KEY_MINIMUM_ATTEMPT_BUDGET, defaults.minimumAttemptBudget()),
                integerSet(config, KEY_RETRYABLE_HTTP_STATUSES, defaults.retryableHttpStatuses()),
                stringSet(config, KEY_RETRYABLE_RPC_STATUSES, defaults.retryableRpcStatuses()),
                bool(config, KEY_RETRY_ONLY_IDEMPOTENT, defaults.retryOnlyIdempotent()));
    }

    private static Map<String, Object> encodeRetry(ServiceCallPolicy.RetryPolicy retry) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(KEY_ENABLED, retry.enabled());
        config.put(KEY_MAX_ATTEMPTS, retry.maxAttempts());
        config.put(KEY_INITIAL_BACKOFF, retry.initialBackoff().toString());
        config.put(KEY_MAXIMUM_BACKOFF, retry.maximumBackoff().toString());
        config.put(KEY_MULTIPLIER, retry.multiplier());
        config.put(KEY_MINIMUM_ATTEMPT_BUDGET, retry.minimumAttemptBudget().toString());
        config.put(KEY_RETRYABLE_HTTP_STATUSES, join(retry.retryableHttpStatuses()));
        config.put(KEY_RETRYABLE_RPC_STATUSES, join(retry.retryableRpcStatuses()));
        config.put(KEY_RETRY_ONLY_IDEMPOTENT, retry.retryOnlyIdempotent());
        return config;
    }

    private static ServiceCallPolicy.LoadBalancePolicy decodeLoadBalance(Map<String, Object> config) {
        ServiceCallPolicy.LoadBalancePolicy defaults = ServiceCallPolicy.LoadBalancePolicy.defaults();
        if (config == null) {
            return defaults;
        }
        LoadBalanceStrategy strategy = LoadBalanceStrategy.fromWire(
                string(config, KEY_STRATEGY, null), defaults.strategy());
        String hashKey = string(config, KEY_HASH_KEY, defaults.hashKey());
        if (strategy == LoadBalanceStrategy.CONSISTENT_HASH && (hashKey == null || hashKey.isBlank())) {
            // 构造该策略会抛错；缺少 hashKey 时降级为默认算法，避免整份规则无法解码。
            strategy = defaults.strategy();
        }
        return new ServiceCallPolicy.LoadBalancePolicy(
                strategy, hashKey, bool(config, KEY_PREFER_SAME_ZONE, defaults.preferSameZone()));
    }

    private static Map<String, Object> encodeLoadBalance(ServiceCallPolicy.LoadBalancePolicy lb) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(KEY_STRATEGY, lb.strategy().name());
        config.put(KEY_HASH_KEY, lb.hashKey());
        config.put(KEY_PREFER_SAME_ZONE, lb.preferSameZone());
        return config;
    }

    private static ServiceCallPolicy.CircuitBreakerPolicy decodeCircuitBreaker(Map<String, Object> config) {
        ServiceCallPolicy.CircuitBreakerPolicy defaults =
                ServiceCallPolicy.CircuitBreakerPolicy.defaults();
        if (config == null) {
            return defaults;
        }
        float threshold = (float) doubleValue(config, KEY_FAILURE_RATE_THRESHOLD,
                defaults.failureRateThreshold());
        if (threshold <= 0f || threshold > 100f) {
            threshold = defaults.failureRateThreshold();
        }
        int windowSize = Math.max(1, integer(config, KEY_SLIDING_WINDOW_SIZE, defaults.slidingWindowSize()));
        int minimumCalls = Math.max(1,
                integer(config, KEY_MINIMUM_NUMBER_OF_CALLS, defaults.minimumNumberOfCalls()));
        return new ServiceCallPolicy.CircuitBreakerPolicy(
                bool(config, KEY_ENABLED, true),
                threshold,
                windowSize,
                Math.min(minimumCalls, windowSize),
                duration(config, KEY_OPEN_DURATION, defaults.openDuration()),
                Math.max(1, integer(config, KEY_HALF_OPEN_PERMITS, defaults.halfOpenPermits())));
    }

    private static Map<String, Object> encodeCircuitBreaker(ServiceCallPolicy.CircuitBreakerPolicy cb) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(KEY_ENABLED, cb.enabled());
        config.put(KEY_FAILURE_RATE_THRESHOLD, cb.failureRateThreshold());
        config.put(KEY_SLIDING_WINDOW_SIZE, cb.slidingWindowSize());
        config.put(KEY_MINIMUM_NUMBER_OF_CALLS, cb.minimumNumberOfCalls());
        config.put(KEY_OPEN_DURATION, cb.openDuration().toString());
        config.put(KEY_HALF_OPEN_PERMITS, cb.halfOpenPermits());
        return config;
    }

    private static ServiceCallPolicy.CachePolicy decodeCache(Map<String, Object> config) {
        if (config == null) {
            return ServiceCallPolicy.CachePolicy.disabled();
        }
        boolean enabled = bool(config, KEY_ENABLED, false);
        Duration ttl = duration(config, KEY_TTL, Duration.ofSeconds(30));
        if (enabled && (ttl.isNegative() || ttl.isZero()
                || ttl.compareTo(ServiceCallPolicy.CachePolicy.MAX_TTL) > 0)) {
            ttl = Duration.ofSeconds(30);
        }
        return new ServiceCallPolicy.CachePolicy(
                enabled, ttl,
                string(config, KEY_KEY_EXPRESSION, ""),
                stringSet(config, KEY_VARY_HEADERS, Set.of()));
    }

    private static Map<String, Object> encodeCache(ServiceCallPolicy.CachePolicy cache) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(KEY_ENABLED, cache.enabled());
        config.put(KEY_TTL, cache.ttl().toString());
        config.put(KEY_KEY_EXPRESSION, cache.keyExpression());
        config.put(KEY_VARY_HEADERS, join(cache.varyHeaders()));
        return config;
    }

    private static String join(Collection<?> values) {
        return values.stream().map(String::valueOf).sorted().collect(Collectors.joining(","));
    }

    private static String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString().trim();
    }

    private static boolean bool(Map<String, Object> config, String key, boolean fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return fallback;
    }

    private static int integer(Map<String, Object> config, String key, int fallback) {
        return (int) longValue(config, key, fallback);
    }

    private static long longValue(Map<String, Object> config, String key, long fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static double doubleValue(Map<String, Object> config, String key, double fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    /**
     * 读取时长，兼容 Engine 已支持的 Duration、毫秒数、ISO-8601 字符串及同级
     * {@code <key>Millis} 字段。
     */
    private static Duration duration(Map<String, Object> config, String key, Duration fallback) {
        Object value = config.get(key);
        if (value == null) {
            Object millis = config.get(key + "Millis");
            if (millis == null) {
                return fallback;
            }
            long parsed = longValue(config, key + "Millis", -1);
            return parsed < 0 ? fallback : Duration.ofMillis(parsed);
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        try {
            return Duration.parse(value.toString().trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private static Set<Integer> integerSet(Map<String, Object> config, String key, Set<Integer> fallback) {
        Set<String> raw = stringSet(config, key, Set.of());
        if (raw.isEmpty()) {
            return fallback;
        }
        Set<Integer> parsed = new LinkedHashSet<>();
        for (String item : raw) {
            try {
                parsed.add(Integer.parseInt(item));
            } catch (NumberFormatException ignored) {
                // 忽略非法项，不丢弃整个状态码列表。
            }
        }
        return parsed.isEmpty() ? fallback : Set.copyOf(parsed);
    }

    private static Set<String> stringSet(Map<String, Object> config, String key, Set<String> fallback) {
        Object value = config.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Collection<?> collection) {
            Set<String> items = collection.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(item -> item.toString().trim())
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return items.isEmpty() ? fallback : Set.copyOf(items);
        }
        Set<String> items = Arrays.stream(value.toString().split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return items.isEmpty() ? fallback : Set.copyOf(items);
    }
}
