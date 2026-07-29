package top.egon.cola.component.accessguard.store.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RedissonPenaltyStore implements PenaltyStore {

    private static final String CURRENT_SCRIPT = """
            -- access-guard:penalty:current
            local values = redis.call('HMGET', KEYS[1], 'violations', 'violationExpiresAt', 'penaltyExpiresAt')
            if not values[1] then return {} end
            local now = redis.call('TIME')
            local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
            local violations = tonumber(values[1])
            local violationExpiresAt = tonumber(values[2])
            local penaltyExpiresAt = tonumber(values[3])
            if penaltyExpiresAt > 0 then
                if penaltyExpiresAt > nowMillis then
                    return {violations, 1, violationExpiresAt, penaltyExpiresAt}
                end
                redis.call('DEL', KEYS[1])
                return {}
            end
            if violationExpiresAt <= nowMillis then
                redis.call('DEL', KEYS[1])
                return {}
            end
            return {violations, 0, violationExpiresAt, 0}
            """;

    private static final String RECORD_SCRIPT = """
            -- access-guard:penalty:record
            local threshold = tonumber(ARGV[1])
            local violationTtlMillis = tonumber(ARGV[2])
            local penaltyTtlMillis = tonumber(ARGV[3])
            local now = redis.call('TIME')
            local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
            local values = redis.call('HMGET', KEYS[1], 'violations', 'violationExpiresAt', 'penaltyExpiresAt')
            local violations = tonumber(values[1]) or 0
            local violationExpiresAt = tonumber(values[2]) or 0
            local penaltyExpiresAt = tonumber(values[3]) or 0
            if penaltyExpiresAt > 0 then
                if penaltyExpiresAt > nowMillis then
                    return {violations, 1, violationExpiresAt, penaltyExpiresAt}
                end
                violations = 0
                violationExpiresAt = 0
                penaltyExpiresAt = 0
            end
            if violationExpiresAt <= nowMillis then
                violations = 0
                violationExpiresAt = nowMillis + violationTtlMillis
            end
            violations = violations + 1
            penaltyExpiresAt = 0
            if violations >= threshold then
                penaltyExpiresAt = nowMillis + penaltyTtlMillis
            end
            redis.call('HSET', KEYS[1],
                    'violations', violations,
                    'violationExpiresAt', violationExpiresAt,
                    'penaltyExpiresAt', penaltyExpiresAt)
            local expiresAt = math.max(violationExpiresAt, penaltyExpiresAt)
            redis.call('PEXPIRE', KEYS[1], math.max(1, expiresAt - nowMillis))
            local active = 0
            if penaltyExpiresAt > nowMillis then active = 1 end
            return {violations, active, violationExpiresAt, penaltyExpiresAt}
            """;

    private final RedissonClient client;
    private final AccessGuardRedisKeyFactory keyFactory;

    public RedissonPenaltyStore(RedissonClient client, AccessGuardRedisKeyFactory keyFactory) {
        this.client = Objects.requireNonNull(client, "client");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
    }

    @Override
    public Optional<PenaltyState> current(PenaltyKey key) {
        Objects.requireNonNull(key, "key");
        List<?> values = eval(CURRENT_SCRIPT, key);
        return values.isEmpty() ? Optional.empty() : Optional.of(state(values));
    }

    @Override
    public PenaltyState recordViolation(
            PenaltyKey key,
            long threshold,
            Duration violationTtl,
            Duration penaltyTtl
    ) {
        Objects.requireNonNull(key, "key");
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        long violationMillis = positiveMillis(violationTtl, "violationTtl");
        long penaltyMillis = positiveMillis(penaltyTtl, "penaltyTtl");
        return state(eval(RECORD_SCRIPT, key, threshold, violationMillis, penaltyMillis));
    }

    private List<?> eval(String script, PenaltyKey key, Object... arguments) {
        try {
            Object result = client.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    script,
                    RScript.ReturnType.MULTI,
                    List.of(keyFactory.penalty(key.ruleId(), key.stateVersion(), key.keyHash())),
                    arguments);
            if (result instanceof List<?> values) {
                return values;
            }
            throw new StoreOperationException("PENALTY_STORE_INVALID_RESPONSE");
        } catch (StoreOperationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StoreOperationException("PENALTY_STORE_FAILED", exception);
        }
    }

    private static PenaltyState state(List<?> values) {
        if (values.size() != 4) {
            throw new StoreOperationException("PENALTY_STORE_INVALID_RESPONSE");
        }
        long violations = number(values.get(0));
        boolean active = number(values.get(1)) == 1L;
        Instant violationExpiresAt = instant(values.get(2));
        Instant penaltyExpiresAt = number(values.get(3)) == 0L ? null : instant(values.get(3));
        return new PenaltyState(violations, active, violationExpiresAt, penaltyExpiresAt);
    }

    private static Instant instant(Object value) {
        return Instant.ofEpochMilli(number(value));
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
