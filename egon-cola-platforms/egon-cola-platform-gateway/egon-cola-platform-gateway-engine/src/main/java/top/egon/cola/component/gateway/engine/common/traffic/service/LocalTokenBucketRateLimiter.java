package top.egon.cola.component.gateway.engine.common.traffic.service;

import top.egon.cola.component.gateway.engine.common.traffic.service.LocalTokenBucketPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.domain.RateLimitDecision;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * 中文说明：{@code LocalTokenBucketRateLimiter} 是类型，位于当前 Gateway 模块的相关包中，负责LocalTokenBucketRateLimiter相关的职责与边界。
 * English summary: {@code LocalTokenBucketRateLimiter} is a type in the current Gateway module; it owns the local token bucket rate limiter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class LocalTokenBucketRateLimiter {

    /**
     * 中文说明：保存 nanoTime 对应的状态、依赖或配置值；字段类型为 {@code LongSupplier}，由 {@code LocalTokenBucketRateLimiter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by nano time; its type is {@code LongSupplier}, and {@code LocalTokenBucketRateLimiter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LongSupplier nanoTime;

    /**
     * 中文说明：保存 epochMillis 对应的状态、依赖或配置值；字段类型为 {@code LongSupplier}，由 {@code LocalTokenBucketRateLimiter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by epoch millis; its type is {@code LongSupplier}, and {@code LocalTokenBucketRateLimiter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LongSupplier epochMillis;

    /**
     * 中文说明：保存 buckets 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Bucket>}，由 {@code LocalTokenBucketRateLimiter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by buckets; its type is {@code Map<String, Bucket>}, and {@code LocalTokenBucketRateLimiter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, Bucket> buckets = new LinkedHashMap<>();

    /**
     * 中文说明：创建 {@code LocalTokenBucketRateLimiter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code LocalTokenBucketRateLimiter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param nanoTime 参数 nanoTime；parameter nano time。
     * @param epochMillis 参数 epochMillis；parameter epoch millis。
     */
    public LocalTokenBucketRateLimiter(
            LongSupplier nanoTime,
            LongSupplier epochMillis) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.epochMillis = Objects.requireNonNull(epochMillis, "epochMillis");
    }

    /**
     * 中文说明：执行 acquire 操作；该方法是 {@code LocalTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the acquire operation; this method is the invocation entry point on {@code LocalTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketRateLimiter.acquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param keyHash 参数 键Hash；parameter key hash。
     * @param permits 参数 permits；parameter permits。
     * @return 返回 acquire 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 stateCount 操作；该方法是 {@code LocalTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the state count operation; this method is the invocation entry point on {@code LocalTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketRateLimiter.stateCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 stateCount 的处理结果；returns the result of the operation.
     */
    public synchronized int stateCount() {
        return buckets.size();
    }

    /**
     * 中文说明：执行 evict 操作；该方法是 {@code LocalTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the evict operation; this method is the invocation entry point on {@code LocalTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketRateLimiter.evict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param now 参数 now；parameter now。
     * @param requestedStateKey 参数 requestedState键；parameter requested state key。
     */
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

    /**
     * 中文说明：{@code Bucket} 是类型，位于当前 Gateway 模块的相关包中，负责Bucket相关的职责与边界。
     * English summary: {@code Bucket} is a type in the current Gateway module; it owns the bucket-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Bucket {

        /**
         * 中文说明：保存 tokens 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketRateLimiter.Bucket} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tokens; its type is {@code long}, and {@code LocalTokenBucketRateLimiter.Bucket} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter.Bucket} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter.Bucket}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long tokens;

        /**
         * 中文说明：保存 lastRefillNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketRateLimiter.Bucket} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last refill nanos; its type is {@code long}, and {@code LocalTokenBucketRateLimiter.Bucket} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter.Bucket} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter.Bucket}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long lastRefillNanos;

        /**
         * 中文说明：保存 lastAccessNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketRateLimiter.Bucket} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last access nanos; its type is {@code long}, and {@code LocalTokenBucketRateLimiter.Bucket} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketRateLimiter.Bucket} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketRateLimiter.Bucket}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long lastAccessNanos;

        /**
         * 中文说明：创建 {@code LocalTokenBucketRateLimiter.Bucket} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code LocalTokenBucketRateLimiter.Bucket} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param tokens 参数 tokens；parameter tokens。
         * @param now 参数 now；parameter now。
         */
        private Bucket(long tokens, long now) {
            this.tokens = tokens;
            lastRefillNanos = now;
            lastAccessNanos = now;
        }

        /**
         * 中文说明：执行 refill 操作；该方法是 {@code LocalTokenBucketRateLimiter.Bucket} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the refill operation; this method is the invocation entry point on {@code LocalTokenBucketRateLimiter.Bucket} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketRateLimiter.Bucket.refill(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param policy 参数 策略；parameter policy。
         * @param now 参数 now；parameter now。
         */
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
