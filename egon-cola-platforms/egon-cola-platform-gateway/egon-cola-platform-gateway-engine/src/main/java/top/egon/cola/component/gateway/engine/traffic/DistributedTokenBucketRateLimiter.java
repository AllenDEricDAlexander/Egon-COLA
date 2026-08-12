package top.egon.cola.component.gateway.engine.traffic;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code DistributedTokenBucketRateLimiter} 是类型，位于当前 Gateway 模块的相关包中，负责DistributedTokenBucketRateLimiter相关的职责与边界。
 * English summary: {@code DistributedTokenBucketRateLimiter} is a type in the current Gateway module; it owns the distributed token bucket rate limiter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DistributedTokenBucketRateLimiter {

    /**
     * 中文说明：表示 TOKENBUCKETLUA 这一固定值；它属于 {@code DistributedTokenBucketRateLimiter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value token bucket lua; it is a state, type, or protocol value of {@code DistributedTokenBucketRateLimiter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code DistributedTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DistributedTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String TOKEN_BUCKET_LUA = """
            local now = redis.call('TIME')
            local nowMs = now[1] * 1000 + math.floor(now[2] / 1000)
            local values = redis.call('HMGET', KEYS[1], 'tokens', 'last')
            local tokens = tonumber(values[1]) or tonumber(ARGV[2])
            local last = tonumber(values[2]) or nowMs
            local periods = math.floor((nowMs - last) / tonumber(ARGV[4]))
            if periods > 0 then
              tokens = math.min(tonumber(ARGV[1]),
                tokens + periods * tonumber(ARGV[3]))
              last = last + periods * tonumber(ARGV[4])
            end
            local allowed = 0
            if tokens >= tonumber(ARGV[5]) then
              tokens = tokens - tonumber(ARGV[5])
              allowed = 1
            end
            local missing = math.max(0, tonumber(ARGV[5]) - tokens)
            local retry = math.ceil(missing / tonumber(ARGV[3]))
              * tonumber(ARGV[4])
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'last', last)
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[6]))
            return {allowed, tokens, retry, nowMs + retry}
            """;

    /**
     * 中文说明：保存 redis 对应的状态、依赖或配置值；字段类型为 {@code RedisTokenBucketExecutor}，由 {@code DistributedTokenBucketRateLimiter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by redis; its type is {@code RedisTokenBucketExecutor}, and {@code DistributedTokenBucketRateLimiter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DistributedTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DistributedTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RedisTokenBucketExecutor redis;

    /**
     * 中文说明：保存 localFallback 对应的状态、依赖或配置值；字段类型为 {@code LocalTokenBucketRateLimiter}，由 {@code DistributedTokenBucketRateLimiter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by local fallback; its type is {@code LocalTokenBucketRateLimiter}, and {@code DistributedTokenBucketRateLimiter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DistributedTokenBucketRateLimiter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DistributedTokenBucketRateLimiter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LocalTokenBucketRateLimiter localFallback;

    /**
     * 中文说明：创建 {@code DistributedTokenBucketRateLimiter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DistributedTokenBucketRateLimiter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param redis 参数 redis；parameter redis。
     * @param localFallback 参数 localFallback；parameter local fallback。
     */
    public DistributedTokenBucketRateLimiter(
            RedisTokenBucketExecutor redis,
            LocalTokenBucketRateLimiter localFallback) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.localFallback = Objects.requireNonNull(
                localFallback,
                "localFallback"
        );
    }

    /**
     * 中文说明：执行 acquire 操作；该方法是 {@code DistributedTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the acquire operation; this method is the invocation entry point on {@code DistributedTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DistributedTokenBucketRateLimiter.acquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param policy 参数 策略；parameter policy。
     * @param keyHash 参数 键Hash；parameter key hash。
     * @param permits 参数 permits；parameter permits。
     * @param failureMode 参数 failureMode；parameter failure mode。
     * @return 返回 acquire 的处理结果；returns the result of the operation.
     */
    public RateLimitDecision acquire(
            String env,
            String namespace,
            LocalTokenBucketPolicy policy,
            String keyHash,
            long permits,
            RateLimitFailureMode failureMode) {
        String redisKey = String.join(
                ":",
                "gateway",
                "ratelimit",
                safe(env),
                safe(namespace),
                safe(policy.policyId()),
                Long.toString(policy.stateEpoch()),
                safeHash(keyHash)
        );
        try {
            List<Long> result = redis.execute(
                    TOKEN_BUCKET_LUA,
                    List.of(redisKey),
                    List.of(
                            Long.toString(policy.capacity()),
                            Long.toString(policy.initialTokens()),
                            Long.toString(policy.refillTokens()),
                            Long.toString(policy.refillPeriod().toMillis()),
                            Long.toString(permits),
                            Long.toString(ttl(policy).toMillis())
                    )
            );
            if (result == null || result.size() != 4) {
                throw new IllegalStateException(
                        "invalid Redis token bucket result"
                );
            }
            return new RateLimitDecision(
                    result.get(0) == 1,
                    result.get(1),
                    result.get(2),
                    result.get(3),
                    false,
                    false
            );
        } catch (RuntimeException unavailable) {
            if (failureMode == RateLimitFailureMode.DENY) {
                return new RateLimitDecision(
                        false,
                        0,
                        0,
                        0,
                        false,
                        true
                );
            }
            RateLimitDecision fallback = localFallback.acquire(
                    policy,
                    keyHash,
                    permits
            );
            return new RateLimitDecision(
                    fallback.allowed(),
                    fallback.remaining(),
                    fallback.retryAfterMillis(),
                    fallback.resetAtEpochMillis(),
                    true,
                    true
            );
        }
    }

    /**
     * 中文说明：执行 ttl 操作；该方法是 {@code DistributedTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ttl operation; this method is the invocation entry point on {@code DistributedTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DistributedTokenBucketRateLimiter.ttl(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 ttl 的处理结果；returns the result of the operation.
     */
    private Duration ttl(LocalTokenBucketPolicy policy) {
        return policy.refillPeriod().multipliedBy(
                Math.max(
                        2,
                        (policy.capacity() + policy.refillTokens() - 1)
                                / policy.refillTokens()
                )
        );
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code DistributedTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code DistributedTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DistributedTokenBucketRateLimiter.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safe 的处理结果；returns the result of the operation.
     */
    private String safe(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("unsafe rate limit dimension");
        }
        return value;
    }

    /**
     * 中文说明：执行 safeHash 操作；该方法是 {@code DistributedTokenBucketRateLimiter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe hash operation; this method is the invocation entry point on {@code DistributedTokenBucketRateLimiter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DistributedTokenBucketRateLimiter.safeHash(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safeHash 的处理结果；returns the result of the operation.
     */
    private String safeHash(String value) {
        if (value == null || !value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("keyHash must be SHA-256");
        }
        return value;
    }
}
