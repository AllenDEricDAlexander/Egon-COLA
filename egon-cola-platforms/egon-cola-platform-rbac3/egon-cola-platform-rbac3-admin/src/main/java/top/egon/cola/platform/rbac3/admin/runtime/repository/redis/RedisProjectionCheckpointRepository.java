package top.egon.cola.platform.rbac3.admin.runtime.repository.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;
import top.egon.cola.platform.rbac3.admin.runtime.repository.internal.Checkpoint;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.RuntimeSnapshotRebuildClaimEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionCheckpointRepository;

/**
 * 类型 `RedisProjectionCheckpointRepository` 位于当前包内，是类型，用于承载 `Redis Projection Checkpoint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RedisProjectionCheckpointRepository` is a type in its package and carries the responsibility, state, or contract for `Redis Projection Checkpoint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Provides multi-node event/version ownership for runtime projection delivery.
 */
@Repository
public class RedisProjectionCheckpointRepository
        implements ProjectionCheckpointRepository {

    /**
     * 字段 `CLAIM_TTL` 表示 `RedisProjectionCheckpointRepository` 中与 `CLAIM TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CLAIM_TTL` stores the `CLAIM TTL`-related state, dependency, configuration, or result of `RedisProjectionCheckpointRepository` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CLAIM_TTL` 时应保持 `RedisProjectionCheckpointRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CLAIM_TTL`, preserve `RedisProjectionCheckpointRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);
    /**
     * 字段 `APPLIED_TTL` 表示 `RedisProjectionCheckpointRepository` 中与 `APPLIED TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `APPLIED_TTL` stores the `APPLIED TTL`-related state, dependency, configuration, or result of `RedisProjectionCheckpointRepository` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `APPLIED_TTL` 时应保持 `RedisProjectionCheckpointRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLIED_TTL`, preserve `RedisProjectionCheckpointRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration APPLIED_TTL = Duration.ofDays(30);
    /**
     * 字段 `KEY_PART` 表示 `RedisProjectionCheckpointRepository` 中与 `KEY PART` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `KEY_PART` stores the `KEY PART`-related state, dependency, configuration, or result of `RedisProjectionCheckpointRepository` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `KEY_PART` 时应保持 `RedisProjectionCheckpointRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `KEY_PART`, preserve `RedisProjectionCheckpointRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern KEY_PART = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    /**
     * 字段 `redisson` 表示 `RedisProjectionCheckpointRepository` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `RedisProjectionCheckpointRepository` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `RedisProjectionCheckpointRepository` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `RedisProjectionCheckpointRepository`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;

    /**
     * 构造器 `RedisProjectionCheckpointRepository` 用于创建并初始化 `RedisProjectionCheckpointRepository` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RedisProjectionCheckpointRepository` creates and initializes `RedisProjectionCheckpointRepository`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RedisProjectionCheckpointRepository` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RedisProjectionCheckpointRepository`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RedisProjectionCheckpointRepository(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    /**
     * 方法 `claim` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateVersion 输入参数 `aggregateVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public RuntimeSnapshotRebuildClaimEnum claim(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        RBucket<String> bucket = bucket(tenantId, aggregateType, aggregateId);
        String pending = value(eventId, aggregateVersion, "PENDING");
        if (bucket.trySet(
                pending, CLAIM_TTL.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return RuntimeSnapshotRebuildClaimEnum.ACQUIRED;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            String current = bucket.get();
            if (current == null) {
                if (bucket.trySet(
                        pending, CLAIM_TTL.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    return RuntimeSnapshotRebuildClaimEnum.ACQUIRED;
                }
                continue;
            }
            Checkpoint parsed = parse(current);
            if (parsed.aggregateVersion() > aggregateVersion) {
                return RuntimeSnapshotRebuildClaimEnum.STALE;
            }
            if (parsed.aggregateVersion() == aggregateVersion) {
                return "APPLIED".equals(parsed.state())
                        ? RuntimeSnapshotRebuildClaimEnum.ALREADY_APPLIED
                        : RuntimeSnapshotRebuildClaimEnum.BUSY;
            }
            if (!"APPLIED".equals(parsed.state())) {
                return RuntimeSnapshotRebuildClaimEnum.BUSY;
            }
            if (bucket.compareAndSet(current, pending)) {
                bucket.expire(CLAIM_TTL);
                return RuntimeSnapshotRebuildClaimEnum.ACQUIRED;
            }
        }
        return RuntimeSnapshotRebuildClaimEnum.BUSY;
    }

    /**
     * 方法 `complete` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateVersion 输入参数 `aggregateVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void complete(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        String pending = value(eventId, aggregateVersion, "PENDING");
        String applied = value(eventId, aggregateVersion, "APPLIED");
        RBucket<String> bucket = bucket(tenantId, aggregateType, aggregateId);
        if (bucket.compareAndSet(pending, applied)) {
            bucket.expire(APPLIED_TTL);
            return;
        }
        throw new IllegalStateException("RBAC3_PROJECTION_CHECKPOINT_OWNERSHIP_LOST");
    }

    /**
     * 方法 `release` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `release` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `release` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `release` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `release` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `release`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateVersion 输入参数 `aggregateVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void release(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        String pending = value(eventId, aggregateVersion, "PENDING");
        bucket(tenantId, aggregateType, aggregateId)
                .compareAndSet(pending, null);
    }

    /**
     * 方法 `bucket` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `bucket` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bucket` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `bucket` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bucket` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bucket`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RBucket<String> bucket(
            String tenantId,
            String aggregateType,
            String aggregateId) {
        return redisson.getBucket(
                "rbac3:{" + part(tenantId) + "}:projection:"
                        + part(aggregateType) + ':' + part(aggregateId),
                StringCodec.INSTANCE);
    }

    /**
     * 方法 `value` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `value` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `value` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `value`, then continue the business flow using its result, exception, or side effect.
     *
     * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String value(String eventId, long version, String state) {
        if (version < 0) {
            throw new IllegalArgumentException("aggregateVersion must not be negative");
        }
        return part(eventId) + '|' + version + '|' + state;
    }

    /**
     * 方法 `parse` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `parse` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parse` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `parse` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parse` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parse`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Checkpoint parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalStateException("RBAC3_PROJECTION_CHECKPOINT_INVALID");
        }
        try {
            return new Checkpoint(parts[0], Long.parseLong(parts[1]), parts[2]);
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException(
                    "RBAC3_PROJECTION_CHECKPOINT_INVALID", invalid);
        }
    }

    /**
     * 方法 `part` 按照 `RedisProjectionCheckpointRepository` 的职责处理输入，完成 `part` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `part` processes its inputs according to `RedisProjectionCheckpointRepository`'s responsibility, performs the `part` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `part` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `part`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String part(String value) {
        if (value == null || !KEY_PART.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid RBAC3 projection key part");
        }
        return value;
    }

    }
