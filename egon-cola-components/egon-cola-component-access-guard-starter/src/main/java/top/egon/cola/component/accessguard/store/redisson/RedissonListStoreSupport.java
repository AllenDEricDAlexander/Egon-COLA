package top.egon.cola.component.accessguard.store.redisson;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class RedissonListStoreSupport {

    private static final String CONTAINS_SCRIPT = """
            -- access-guard:list:contains
            local expiresAt = redis.call('HGET', KEYS[1], ARGV[1])
            if not expiresAt then return 0 end
            if tonumber(expiresAt) == 0 then return 1 end
            local now = redis.call('TIME')
            local nowMillis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
            if tonumber(expiresAt) <= nowMillis then
                redis.call('HDEL', KEYS[1], ARGV[1])
                return 0
            end
            return 1
            """;

    private static final String ADD_SCRIPT = """
            -- access-guard:list:add
            local ttlMillis = tonumber(ARGV[2])
            local expiresAt = 0
            if ttlMillis > 0 then
                local now = redis.call('TIME')
                expiresAt = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000) + ttlMillis
            end
            redis.call('HSET', KEYS[1], ARGV[1], expiresAt)
            return 1
            """;

    private static final String REMOVE_SCRIPT = """
            -- access-guard:list:remove
            return redis.call('HDEL', KEYS[1], ARGV[1])
            """;

    private static final String REPLACE_SCRIPT = """
            -- access-guard:list:replace
            redis.call('DEL', KEYS[1])
            if #ARGV == 1 then return 1 end
            local ttlMillis = tonumber(ARGV[1])
            local expiresAt = 0
            if ttlMillis > 0 then
                local now = redis.call('TIME')
                expiresAt = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000) + ttlMillis
            end
            for index = 2, #ARGV do
                redis.call('HSET', KEYS[1], ARGV[index], expiresAt)
            end
            return 1
            """;

    private final RedissonClient client;

    RedissonListStoreSupport(RedissonClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    boolean contains(String redisKey, String keyHash) {
        requireHash(keyHash);
        Object result = eval(CONTAINS_SCRIPT, RScript.ReturnType.INTEGER, redisKey, keyHash);
        return number(result) == 1L;
    }

    void add(String redisKey, String keyHash, Duration ttl) {
        requireHash(keyHash);
        eval(ADD_SCRIPT, RScript.ReturnType.INTEGER, redisKey, keyHash, ttlMillis(ttl));
    }

    void remove(String redisKey, String keyHash) {
        requireHash(keyHash);
        eval(REMOVE_SCRIPT, RScript.ReturnType.INTEGER, redisKey, keyHash);
    }

    void replace(String redisKey, Set<String> keyHashes, Duration ttl) {
        Set<String> values = Set.copyOf(Objects.requireNonNull(keyHashes, "keyHashes"));
        values.forEach(RedissonListStoreSupport::requireHash);
        List<Object> arguments = new ArrayList<>(values.size() + 1);
        arguments.add(ttlMillis(ttl));
        values.stream().sorted(Comparator.naturalOrder()).forEach(arguments::add);
        eval(REPLACE_SCRIPT, RScript.ReturnType.INTEGER, redisKey, arguments.toArray());
    }

    private Object eval(String script, RScript.ReturnType returnType, String redisKey, Object... arguments) {
        try {
            return client.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    script,
                    returnType,
                    List.of(redisKey),
                    arguments);
        } catch (RuntimeException exception) {
            throw new StoreOperationException("LIST_STORE_FAILED", exception);
        }
    }

    private static long ttlMillis(Duration ttl) {
        Duration value = ttl == null ? Duration.ZERO : ttl;
        if (value.isNegative()) {
            throw new IllegalArgumentException("ttl must not be negative");
        }
        if (value.isZero()) {
            return 0L;
        }
        return Math.max(1L, value.toMillis());
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new StoreOperationException("LIST_STORE_INVALID_RESPONSE");
    }

    private static void requireHash(String keyHash) {
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
    }
}
