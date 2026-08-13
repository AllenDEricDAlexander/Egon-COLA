package top.egon.cola.platform.rbac3.admin.worker;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 类型 `RedisProjectionCheckpointStore` 位于当前包内，是类型，用于承载 `Redis Projection Checkpoint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RedisProjectionCheckpointStore` is a type in its package and carries the responsibility, state, or contract for `Redis Projection Checkpoint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Provides multi-node event/version ownership for runtime projection delivery.
 */
@Repository
public class RedisProjectionCheckpointStore
        implements RuntimeSnapshotRebuildWorker.ProjectionCheckpointStore {

    /**
     * 字段 `CLAIM_TTL` 表示 `RedisProjectionCheckpointStore` 中与 `CLAIM TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CLAIM_TTL` stores the `CLAIM TTL`-related state, dependency, configuration, or result of `RedisProjectionCheckpointStore` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CLAIM_TTL` 时应保持 `RedisProjectionCheckpointStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CLAIM_TTL`, preserve `RedisProjectionCheckpointStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);
    /**
     * 字段 `APPLIED_TTL` 表示 `RedisProjectionCheckpointStore` 中与 `APPLIED TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `APPLIED_TTL` stores the `APPLIED TTL`-related state, dependency, configuration, or result of `RedisProjectionCheckpointStore` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `APPLIED_TTL` 时应保持 `RedisProjectionCheckpointStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLIED_TTL`, preserve `RedisProjectionCheckpointStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration APPLIED_TTL = Duration.ofDays(30);
    /**
     * 字段 `KEY_PART` 表示 `RedisProjectionCheckpointStore` 中与 `KEY PART` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `KEY_PART` stores the `KEY PART`-related state, dependency, configuration, or result of `RedisProjectionCheckpointStore` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `KEY_PART` 时应保持 `RedisProjectionCheckpointStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `KEY_PART`, preserve `RedisProjectionCheckpointStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern KEY_PART = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    /**
     * 字段 `redisson` 表示 `RedisProjectionCheckpointStore` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `RedisProjectionCheckpointStore` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `RedisProjectionCheckpointStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `RedisProjectionCheckpointStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;

    /**
     * 构造器 `RedisProjectionCheckpointStore` 用于创建并初始化 `RedisProjectionCheckpointStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RedisProjectionCheckpointStore` creates and initializes `RedisProjectionCheckpointStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RedisProjectionCheckpointStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RedisProjectionCheckpointStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RedisProjectionCheckpointStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    /**
     * 方法 `claim` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public RuntimeSnapshotRebuildWorker.Claim claim(
            String tenantId,
            String eventId,
            String aggregateType,
            String aggregateId,
            long aggregateVersion) {
        RBucket<String> bucket = bucket(tenantId, aggregateType, aggregateId);
        String pending = value(eventId, aggregateVersion, "PENDING");
        if (bucket.trySet(
                pending, CLAIM_TTL.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            String current = bucket.get();
            if (current == null) {
                if (bucket.trySet(
                        pending, CLAIM_TTL.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
                }
                continue;
            }
            Checkpoint parsed = parse(current);
            if (parsed.aggregateVersion() > aggregateVersion) {
                return RuntimeSnapshotRebuildWorker.Claim.STALE;
            }
            if (parsed.aggregateVersion() == aggregateVersion) {
                return "APPLIED".equals(parsed.state())
                        ? RuntimeSnapshotRebuildWorker.Claim.ALREADY_APPLIED
                        : RuntimeSnapshotRebuildWorker.Claim.BUSY;
            }
            if (!"APPLIED".equals(parsed.state())) {
                return RuntimeSnapshotRebuildWorker.Claim.BUSY;
            }
            if (bucket.compareAndSet(current, pending)) {
                bucket.expire(CLAIM_TTL);
                return RuntimeSnapshotRebuildWorker.Claim.ACQUIRED;
            }
        }
        return RuntimeSnapshotRebuildWorker.Claim.BUSY;
    }

    /**
     * 方法 `complete` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `release` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `release` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `release` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `release` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `bucket` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `bucket` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bucket` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `bucket` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `value` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `value` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `parse` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `parse` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parse` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `parse` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `part` 按照 `RedisProjectionCheckpointStore` 的职责处理输入，完成 `part` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `part` processes its inputs according to `RedisProjectionCheckpointStore`'s responsibility, performs the `part` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `Checkpoint` 位于 `RedisProjectionCheckpointStore` 内，是记录类型，用于承载 `Checkpoint` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Checkpoint` is a record inside `RedisProjectionCheckpointStore` and carries the responsibility, state, or contract for `Checkpoint`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Checkpoint` 作为 `RedisProjectionCheckpointStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Checkpoint` as the responsibility boundary of `RedisProjectionCheckpointStore`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param eventId 记录组件 `eventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateVersion 记录组件 `aggregateVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateVersion` carries constructor data whose meaning is defined by the record contract.
     * @param state 记录组件 `state` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `state` carries constructor data whose meaning is defined by the record contract.
     */
    private record Checkpoint(/**
 * 字段 `eventId` 表示 `Checkpoint` 中与 `event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `eventId` stores the `event Id`-related state, dependency, configuration, or result of `Checkpoint` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `eventId` 时应保持 `Checkpoint` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `eventId`, preserve `Checkpoint`'s lifecycle, immutability, and thread-safety constraints.
 */ String eventId, /**
 * 字段 `aggregateVersion` 表示 `Checkpoint` 中与 `aggregate Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `aggregateVersion` stores the `aggregate Version`-related state, dependency, configuration, or result of `Checkpoint` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `aggregateVersion` 时应保持 `Checkpoint` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `aggregateVersion`, preserve `Checkpoint`'s lifecycle, immutability, and thread-safety constraints.
 */ long aggregateVersion, /**
 * 字段 `state` 表示 `Checkpoint` 中与 `state` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `state` stores the `state`-related state, dependency, configuration, or result of `Checkpoint` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `state` 时应保持 `Checkpoint` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `state`, preserve `Checkpoint`'s lifecycle, immutability, and thread-safety constraints.
 */ String state) {
    }
}
