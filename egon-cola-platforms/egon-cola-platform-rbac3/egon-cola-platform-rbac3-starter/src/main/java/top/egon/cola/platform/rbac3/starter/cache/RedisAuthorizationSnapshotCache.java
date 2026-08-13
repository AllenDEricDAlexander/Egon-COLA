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

/**
 * 类型 `RedisAuthorizationSnapshotCache` 位于当前包内，是类型，用于承载 `Redis Authorization Snapshot Cache` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RedisAuthorizationSnapshotCache` is a type in its package and carries the responsibility, state, or contract for `Redis Authorization Snapshot Cache`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Redis adapter for one application's authorization cache and exact indexes.
 */
public final class RedisAuthorizationSnapshotCache
        implements AuthorizationSnapshotCache.SnapshotStore {

    /**
     * 字段 `PUT_SCRIPT` 表示 `RedisAuthorizationSnapshotCache` 中与 `PUT SCRIPT` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PUT_SCRIPT` stores the `PUT SCRIPT`-related state, dependency, configuration, or result of `RedisAuthorizationSnapshotCache` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PUT_SCRIPT` 时应保持 `RedisAuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PUT_SCRIPT`, preserve `RedisAuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String PUT_SCRIPT = """
            redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('sadd', KEYS[2], KEYS[1])
            redis.call('pexpire', KEYS[2], ARGV[3])
            redis.call('sadd', KEYS[3], KEYS[1])
            redis.call('pexpire', KEYS[3], ARGV[3])
            return 1
            """;

    /**
     * 字段 `redisson` 表示 `RedisAuthorizationSnapshotCache` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `RedisAuthorizationSnapshotCache` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `RedisAuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `RedisAuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;
    /**
     * 字段 `objectMapper` 表示 `RedisAuthorizationSnapshotCache` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `RedisAuthorizationSnapshotCache` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `RedisAuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `RedisAuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `maximumJitter` 表示 `RedisAuthorizationSnapshotCache` 中与 `maximum Jitter` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumJitter` stores the `maximum Jitter`-related state, dependency, configuration, or result of `RedisAuthorizationSnapshotCache` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumJitter` 时应保持 `RedisAuthorizationSnapshotCache` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumJitter`, preserve `RedisAuthorizationSnapshotCache`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration maximumJitter;

    /**
     * 构造器 `RedisAuthorizationSnapshotCache` 用于创建并初始化 `RedisAuthorizationSnapshotCache` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RedisAuthorizationSnapshotCache` creates and initializes `RedisAuthorizationSnapshotCache`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RedisAuthorizationSnapshotCache` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RedisAuthorizationSnapshotCache`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumJitter 输入参数 `maximumJitter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `get` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `get` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `put` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `put` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `invalidate` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidate` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `invalidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `invalidateUser` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate User` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidateUser` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `invalidate User` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidateUser` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidateUser`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `invalidateTenant` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `invalidate Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalidateTenant` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `invalidate Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalidateTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalidateTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param systemCode 输入参数 `systemCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `deleteData` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `delete Data` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `deleteData` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `delete Data` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `deleteData` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `deleteData`, then continue the business flow using its result, exception, or side effect.
     *
     * @param keys 输入参数 `keys`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void deleteData(List<String> keys) {
        if (!keys.isEmpty()) {
            redisson.getKeys().delete(keys.toArray(String[]::new));
        }
    }

    /**
     * 方法 `jitterMillis` 按照 `RedisAuthorizationSnapshotCache` 的职责处理输入，完成 `jitter Millis` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `jitterMillis` processes its inputs according to `RedisAuthorizationSnapshotCache`'s responsibility, performs the `jitter Millis` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `jitterMillis` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `jitterMillis`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long jitterMillis() {
        long maximum = maximumJitter.toMillis();
        return maximum == 0 ? 0 : ThreadLocalRandom.current().nextLong(maximum + 1);
    }
}
