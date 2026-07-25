package top.egon.cola.component.gateway.engine.traffic;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class DistributedTokenBucketRateLimiter {

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

    private final RedisTokenBucketExecutor redis;

    private final LocalTokenBucketRateLimiter localFallback;

    public DistributedTokenBucketRateLimiter(
            RedisTokenBucketExecutor redis,
            LocalTokenBucketRateLimiter localFallback) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.localFallback = Objects.requireNonNull(
                localFallback,
                "localFallback"
        );
    }

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

    private Duration ttl(LocalTokenBucketPolicy policy) {
        return policy.refillPeriod().multipliedBy(
                Math.max(
                        2,
                        (policy.capacity() + policy.refillTokens() - 1)
                                / policy.refillTokens()
                )
        );
    }

    private String safe(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("unsafe rate limit dimension");
        }
        return value;
    }

    private String safeHash(String value) {
        if (value == null || !value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("keyHash must be SHA-256");
        }
        return value;
    }
}
