package top.egon.cola.component.accessguard.store.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitAlgorithmStrategy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitAlgorithmStrategyFactory;
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

    private static final String LEAKY_BUCKET_SCRIPT = """
            -- access-guard:rate-limit:leaky-bucket
            local capacity = tonumber(ARGV[1])
            local leakTokens = tonumber(ARGV[2])
            local leakPeriodMillis = tonumber(ARGV[3])
            local requestedTokens = tonumber(ARGV[4])
            local idleTtlMillis = tonumber(ARGV[5])
            local redisTime = redis.call('TIME')
            local nowMillis = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
            local state = redis.call('HMGET', KEYS[1], 'level', 'lastLeak', 'capacity', 'leakTokens', 'leakPeriod')
            local level = tonumber(state[1])
            local lastLeak = tonumber(state[2])
            if not level
                    or tonumber(state[3]) ~= capacity
                    or tonumber(state[4]) ~= leakTokens
                    or tonumber(state[5]) ~= leakPeriodMillis then
                level = 0
                lastLeak = nowMillis
            else
                local elapsed = math.max(0, nowMillis - lastLeak)
                local periods = math.floor(elapsed / leakPeriodMillis)
                if periods > 0 then
                    level = math.max(0, level - periods * leakTokens)
                    lastLeak = lastLeak + periods * leakPeriodMillis
                end
            end
            local allowed = 0
            local retryAfterMillis = 0
            if level + requestedTokens <= capacity then
                allowed = 1
                level = level + requestedTokens
            else
                local missing = level + requestedTokens - capacity
                local periodsNeeded = math.ceil(missing / leakTokens)
                retryAfterMillis = math.max(0,
                        periodsNeeded * leakPeriodMillis - (nowMillis - lastLeak))
            end
            redis.call('HSET', KEYS[1],
                    'level', level,
                    'lastLeak', lastLeak,
                    'capacity', capacity,
                    'leakTokens', leakTokens,
                    'leakPeriod', leakPeriodMillis)
            redis.call('PEXPIRE', KEYS[1], idleTtlMillis)
            return {allowed, capacity - level, retryAfterMillis}
            """;

    private static final String SLIDING_WINDOW_SCRIPT = """
            -- access-guard:rate-limit:sliding-window
            local capacity = tonumber(ARGV[1])
            local windowMillis = tonumber(ARGV[3])
            local idleTtlMillis = tonumber(ARGV[5])
            local redisTime = redis.call('TIME')
            local nowMillis = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)
            local boundary = nowMillis - windowMillis
            local oldest = redis.call('LINDEX', KEYS[1], 0)
            if oldest then
                local version, existingCapacity, existingWindow =
                        string.match(oldest, '^([^|]+)|([^|]+)|([^|]+)|([^|]+)$')
                if version ~= 'v1'
                        or tonumber(existingCapacity) ~= capacity
                        or tonumber(existingWindow) ~= windowMillis then
                    redis.call('DEL', KEYS[1])
                    oldest = false
                end
            end
            while oldest do
                local _, _, _, timestamp =
                        string.match(oldest, '^([^|]+)|([^|]+)|([^|]+)|([^|]+)$')
                local oldestMillis = tonumber(timestamp)
                if not oldestMillis or oldestMillis <= boundary then
                    redis.call('LPOP', KEYS[1])
                    oldest = redis.call('LINDEX', KEYS[1], 0)
                else
                    break
                end
            end
            local length = redis.call('LLEN', KEYS[1])
            local allowed = 0
            local remaining = 0
            local retryAfterMillis = 0
            if length < capacity then
                redis.call('RPUSH', KEYS[1], 'v1|' .. capacity .. '|' .. windowMillis .. '|' .. nowMillis)
                allowed = 1
                remaining = capacity - length - 1
            else
                local _, _, _, timestamp =
                        string.match(oldest, '^([^|]+)|([^|]+)|([^|]+)|([^|]+)$')
                local oldestMillis = tonumber(timestamp)
                retryAfterMillis = math.max(0, oldestMillis + windowMillis - nowMillis)
            end
            redis.call('PEXPIRE', KEYS[1], idleTtlMillis)
            return {allowed, remaining, retryAfterMillis}
            """;

    private final RedissonClient client;
    private final AccessGuardRedisKeyFactory keyFactory;
    private final long idleTtlMillis;
    private final RateLimitAlgorithmStrategyFactory strategies;

    public RedissonRateLimitBackend(
            RedissonClient client,
            AccessGuardRedisKeyFactory keyFactory,
            Duration idleTtl
    ) {
        this.client = Objects.requireNonNull(client, "client");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.idleTtlMillis = positiveMillis(idleTtl, "idleTtl");
        this.strategies = new RateLimitAlgorithmStrategyFactory(List.of(
                new TokenBucketStrategy(),
                new LeakyBucketStrategy(),
                new SlidingWindowStrategy()));
    }

    @Override
    public RateLimitDecision acquire(RateLimitRequest request) {
        return strategies.acquire(Objects.requireNonNull(request, "request"));
    }

    private RateLimitDecision execute(
            RateLimitRequest request,
            String script) {
        try {
            Object result = client.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    script,
                    RScript.ReturnType.MULTI,
                    List.of(keyFactory.rateLimit(
                            request.ruleId(), request.stateVersion(), request.keyHash(),
                            request.algorithm())),
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

    private abstract class RedisRateLimitStrategy implements RateLimitAlgorithmStrategy {

        protected abstract String script();

        @Override
        public RateLimitDecision acquire(RateLimitRequest request) {
            if (request.algorithm() != algorithm()) {
                throw new IllegalArgumentException(
                        "rate-limit algorithm mismatch: expected "
                                + algorithm() + " but was " + request.algorithm());
            }
            return execute(request, script());
        }
    }

    private final class TokenBucketStrategy extends RedisRateLimitStrategy {

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET;
        }

        @Override
        protected String script() {
            return ACQUIRE_SCRIPT;
        }
    }

    private final class LeakyBucketStrategy extends RedisRateLimitStrategy {

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.LEAKY_BUCKET;
        }

        @Override
        protected String script() {
            return LEAKY_BUCKET_SCRIPT;
        }
    }

    private final class SlidingWindowStrategy extends RedisRateLimitStrategy {

        @Override
        public AdmissionConfig.RateLimitAlgorithm algorithm() {
            return AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW;
        }

        @Override
        protected String script() {
            return SLIDING_WINDOW_SCRIPT;
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
