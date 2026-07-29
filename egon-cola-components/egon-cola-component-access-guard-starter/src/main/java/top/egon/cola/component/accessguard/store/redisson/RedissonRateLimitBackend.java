package top.egon.cola.component.accessguard.store.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class RedissonRateLimitBackend implements RateLimitBackend {

    private static final String ACQUIRE_SCRIPT = """
            -- access-guard:rate-limit:acquire
            local capacity = tonumber(ARGV[1])
            local refillTokens = tonumber(ARGV[2])
            local refillPeriodMillis = tonumber(ARGV[3])
            local requestedTokens = tonumber(ARGV[4])
            local idleTtlMillis = tonumber(ARGV[5])
            local redisTime = redis.call('TIME')
            local nowMillis = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
            local state = redis.call('HMGET', KEYS[1], 'tokens', 'lastRefill', 'capacity', 'refillTokens', 'refillPeriod')
            local tokens = tonumber(state[1])
            local lastRefill = tonumber(state[2])
            if not tokens
                    or tonumber(state[3]) ~= capacity
                    or tonumber(state[4]) ~= refillTokens
                    or tonumber(state[5]) ~= refillPeriodMillis then
                tokens = capacity
                lastRefill = nowMillis
            else
                local elapsed = math.max(0, nowMillis - lastRefill)
                local periods = math.floor(elapsed / refillPeriodMillis)
                if periods > 0 then
                    tokens = math.min(capacity, tokens + periods * refillTokens)
                    lastRefill = lastRefill + periods * refillPeriodMillis
                end
            end
            local allowed = 0
            local retryAfterMillis = 0
            if tokens >= requestedTokens then
                allowed = 1
                tokens = tokens - requestedTokens
            else
                local missing = requestedTokens - tokens
                local periodsNeeded = math.ceil(missing / refillTokens)
                retryAfterMillis = math.max(0,
                        periodsNeeded * refillPeriodMillis - (nowMillis - lastRefill))
            end
            redis.call('HSET', KEYS[1],
                    'tokens', tokens,
                    'lastRefill', lastRefill,
                    'capacity', capacity,
                    'refillTokens', refillTokens,
                    'refillPeriod', refillPeriodMillis)
            redis.call('PEXPIRE', KEYS[1], idleTtlMillis)
            return {allowed, tokens, retryAfterMillis}
            """;

    private final RedissonClient client;
    private final AccessGuardRedisKeyFactory keyFactory;
    private final long idleTtlMillis;

    public RedissonRateLimitBackend(
            RedissonClient client,
            AccessGuardRedisKeyFactory keyFactory,
            Duration idleTtl
    ) {
        this.client = Objects.requireNonNull(client, "client");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.idleTtlMillis = positiveMillis(idleTtl, "idleTtl");
    }

    @Override
    public RateLimitDecision acquire(RateLimitRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            Object result = client.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    ACQUIRE_SCRIPT,
                    RScript.ReturnType.MULTI,
                    List.of(keyFactory.rateLimit(
                            request.ruleId(), request.stateVersion(), request.keyHash())),
                    request.capacity(),
                    request.refillTokens(),
                    positiveMillis(request.refillPeriod(), "refillPeriod"),
                    request.requestedTokens(),
                    idleTtlMillis);
            if (!(result instanceof List<?> values) || values.size() != 3) {
                throw new StoreOperationException("RATE_LIMIT_STORE_INVALID_RESPONSE");
            }
            boolean allowed = number(values.get(0)) == 1L;
            long remaining = number(values.get(1));
            long retryAfterMillis = number(values.get(2));
            return new RateLimitDecision(
                    allowed,
                    remaining,
                    allowed ? Duration.ZERO : Duration.ofMillis(retryAfterMillis));
        } catch (StoreOperationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StoreOperationException("RATE_LIMIT_STORE_FAILED", exception);
        }
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof byte[] bytes) {
            return Long.parseLong(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static long positiveMillis(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return Math.max(1L, value.toMillis());
    }
}
