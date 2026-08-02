package top.egon.cola.platform.rbac3.starter.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Redis adapter for one application's authorization cache and exact indexes. */
public final class RedisAuthorizationSnapshotCache
        implements AuthorizationSnapshotCache.SnapshotStore {

    private static final String PUT_SCRIPT = """
            redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('sadd', KEYS[2], KEYS[1])
            redis.call('pexpire', KEYS[2], ARGV[3])
            redis.call('sadd', KEYS[3], KEYS[1])
            redis.call('pexpire', KEYS[3], ARGV[3])
            return 1
            """;

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Duration maximumJitter;

    public RedisAuthorizationSnapshotCache(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            Duration maximumJitter) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.maximumJitter = Objects.requireNonNull(maximumJitter, "maximumJitter");
        if (maximumJitter.isNegative()
                || maximumJitter.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("maximumJitter is outside the safe range");
        }
    }

    @Override
    public Optional<SystemAuthorizationSnapshot> get(
            AuthorizationSnapshotCache.Key key) {
        String json = redisson.<String>getBucket(
                key.redisKey(), StringCodec.INSTANCE).get();
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(
                    json, SystemAuthorizationSnapshot.class));
        } catch (JsonProcessingException exception) {
            redisson.<String>getBucket(key.redisKey(), StringCodec.INSTANCE).delete();
            throw new IllegalStateException(
                    "RBAC3 authorization cache value is invalid", exception);
        }
    }

    @Override
    public void put(
            AuthorizationSnapshotCache.Key key,
            SystemAuthorizationSnapshot snapshot,
            Duration ttl) {
        long dataTtl = Math.addExact(ttl.toMillis(), jitterMillis());
        long indexTtl = Math.addExact(dataTtl, Duration.ofMinutes(1).toMillis());
        try {
            Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    PUT_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    List.of(key.redisKey(), key.userIndex(snapshot.identitySub()),
                            key.tenantIndex()),
                    objectMapper.writeValueAsString(snapshot),
                    Long.toString(dataTtl), Long.toString(indexTtl));
            if (result == null || result.longValue() != 1L) {
                throw new IllegalStateException(
                        "RBAC3 authorization cache write was not acknowledged");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Cannot encode RBAC3 authorization snapshot", exception);
        }
    }

    @Override
    public void invalidate(AuthorizationSnapshotCache.Key key) {
        Optional<SystemAuthorizationSnapshot> existing = get(key);
        redisson.<String>getBucket(key.redisKey(), StringCodec.INSTANCE).delete();
        redisson.<String>getSet(key.tenantIndex(), StringCodec.INSTANCE)
                .remove(key.redisKey());
        existing.ifPresent(snapshot -> redisson.<String>getSet(
                        key.userIndex(snapshot.identitySub()), StringCodec.INSTANCE)
                .remove(key.redisKey()));
    }

    @Override
    public void invalidateUser(
            String systemCode,
            String tenantId,
            String identitySub) {
        AuthorizationSnapshotCache.Key indexKey =
                new AuthorizationSnapshotCache.Key(systemCode, tenantId, "index");
        String userIndex = indexKey.userIndex(identitySub);
        List<String> keys = new ArrayList<>(redisson.<String>getSet(
                userIndex, StringCodec.INSTANCE).readAll());
        deleteData(keys);
        redisson.<String>getSet(indexKey.tenantIndex(), StringCodec.INSTANCE)
                .removeAll(keys);
        redisson.<String>getSet(userIndex, StringCodec.INSTANCE).delete();
    }

    @Override
    public void invalidateTenant(String systemCode, String tenantId) {
        AuthorizationSnapshotCache.Key indexKey =
                new AuthorizationSnapshotCache.Key(systemCode, tenantId, "index");
        List<String> keys = new ArrayList<>(redisson.<String>getSet(
                indexKey.tenantIndex(), StringCodec.INSTANCE).readAll());
        for (String key : keys) {
            String json = redisson.<String>getBucket(key, StringCodec.INSTANCE).get();
            if (json != null) {
                try {
                    SystemAuthorizationSnapshot snapshot = objectMapper.readValue(
                            json, SystemAuthorizationSnapshot.class);
                    redisson.<String>getSet(indexKey.userIndex(
                                    snapshot.identitySub()), StringCodec.INSTANCE)
                            .remove(key);
                } catch (JsonProcessingException ignored) {
                    // The data key is deleted below; malformed cache data is not trusted.
                }
            }
        }
        deleteData(keys);
        redisson.<String>getSet(indexKey.tenantIndex(), StringCodec.INSTANCE).delete();
    }

    private void deleteData(List<String> keys) {
        if (!keys.isEmpty()) {
            redisson.getKeys().delete(keys.toArray(String[]::new));
        }
    }

    private long jitterMillis() {
        long maximum = maximumJitter.toMillis();
        return maximum == 0 ? 0 : ThreadLocalRandom.current().nextLong(maximum + 1);
    }
}
