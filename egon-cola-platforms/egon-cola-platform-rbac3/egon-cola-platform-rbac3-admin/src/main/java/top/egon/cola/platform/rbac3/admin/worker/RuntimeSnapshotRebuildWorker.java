package top.egon.cola.platform.rbac3.admin.worker;

import top.egon.cola.platform.rbac3.admin.integration.outbox.Rbac3RuntimeProjectionDeliveryHandler;

import java.util.Objects;

/**
 * 类型 `RuntimeSnapshotRebuildWorker` 位于当前包内，是类型，用于承载 `Runtime Snapshot Rebuild Worker` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RuntimeSnapshotRebuildWorker` is a type in its package and carries the responsibility, state, or contract for `Runtime Snapshot Rebuild Worker`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Idempotently rebuilds runtime projections by stable event and aggregate version.
 */
public final class RuntimeSnapshotRebuildWorker
        implements Rbac3RuntimeProjectionDeliveryHandler.ProjectionSink {

    /**
     * 字段 `checkpoints` 表示 `RuntimeSnapshotRebuildWorker` 中与 `checkpoints` 相关的状态、依赖、配置或结果（声明类型 `ProjectionCheckpointStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `checkpoints` stores the `checkpoints`-related state, dependency, configuration, or result of `RuntimeSnapshotRebuildWorker` (declared type `ProjectionCheckpointStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `checkpoints` 时应保持 `RuntimeSnapshotRebuildWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `checkpoints`, preserve `RuntimeSnapshotRebuildWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ProjectionCheckpointStore checkpoints;
    /**
     * 字段 `rebuildPort` 表示 `RuntimeSnapshotRebuildWorker` 中与 `rebuild Port` 相关的状态、依赖、配置或结果（声明类型 `RebuildPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `rebuildPort` stores the `rebuild Port`-related state, dependency, configuration, or result of `RuntimeSnapshotRebuildWorker` (declared type `RebuildPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `rebuildPort` 时应保持 `RuntimeSnapshotRebuildWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `rebuildPort`, preserve `RuntimeSnapshotRebuildWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RebuildPort rebuildPort;

    /**
     * 构造器 `RuntimeSnapshotRebuildWorker` 用于创建并初始化 `RuntimeSnapshotRebuildWorker` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RuntimeSnapshotRebuildWorker` creates and initializes `RuntimeSnapshotRebuildWorker`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RuntimeSnapshotRebuildWorker` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RuntimeSnapshotRebuildWorker`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param checkpoints 输入参数 `checkpoints`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rebuildPort 输入参数 `rebuildPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RuntimeSnapshotRebuildWorker(
            ProjectionCheckpointStore checkpoints,
            RebuildPort rebuildPort) {
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.rebuildPort = Objects.requireNonNull(rebuildPort, "rebuildPort");
    }

    /**
     * 方法 `project` 按照 `RuntimeSnapshotRebuildWorker` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `project` processes its inputs according to `RuntimeSnapshotRebuildWorker`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome project(
            Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event) {
        Claim claim = checkpoints.claim(
                event.tenantId(), event.eventId(), event.aggregateType(),
                event.aggregateId(), event.aggregateVersion());
        if (claim == Claim.ALREADY_APPLIED) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.ALREADY_APPLIED;
        }
        if (claim == Claim.STALE) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.PERMANENT_FAILURE;
        }
        if (claim == Claim.BUSY) {
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.RETRYABLE_FAILURE;
        }
        try {
            rebuildPort.rebuild(event);
            checkpoints.complete(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandler.ProjectionOutcome.APPLIED;
        } catch (RuntimeException unavailable) {
            checkpoints.release(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandler
                    .ProjectionOutcome.RETRYABLE_FAILURE;
        }
    }

    /**
     * 类型 `Claim` 位于 `RuntimeSnapshotRebuildWorker` 内，是枚举，用于承载 `Claim` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Claim` is an enum inside `RuntimeSnapshotRebuildWorker` and carries the responsibility, state, or contract for `Claim`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Claim` 作为 `RuntimeSnapshotRebuildWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Claim` as the responsibility boundary of `RuntimeSnapshotRebuildWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Claim {
        /**
         * 字段 `ACQUIRED` 表示 `Claim` 中与 `ACQUIRED` 相关的状态、依赖、配置或结果（声明类型 `Claim`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACQUIRED` stores the `ACQUIRED`-related state, dependency, configuration, or result of `Claim` (declared type `Claim`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACQUIRED` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACQUIRED`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACQUIRED,
        /**
         * 字段 `ALREADY_APPLIED` 表示 `Claim` 中与 `ALREADY APPLIED` 相关的状态、依赖、配置或结果（声明类型 `Claim`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ALREADY_APPLIED` stores the `ALREADY APPLIED`-related state, dependency, configuration, or result of `Claim` (declared type `Claim`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ALREADY_APPLIED` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ALREADY_APPLIED`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
         */
        ALREADY_APPLIED,
        /**
         * 字段 `STALE` 表示 `Claim` 中与 `STALE` 相关的状态、依赖、配置或结果（声明类型 `Claim`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `STALE` stores the `STALE`-related state, dependency, configuration, or result of `Claim` (declared type `Claim`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `STALE` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `STALE`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
         */
        STALE,
        /**
         * 字段 `BUSY` 表示 `Claim` 中与 `BUSY` 相关的状态、依赖、配置或结果（声明类型 `Claim`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `BUSY` stores the `BUSY`-related state, dependency, configuration, or result of `Claim` (declared type `Claim`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `BUSY` 时应保持 `Claim` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `BUSY`, preserve `Claim`'s lifecycle, immutability, and thread-safety constraints.
         */
        BUSY
    }

    /**
     * 类型 `ProjectionCheckpointStore` 位于 `RuntimeSnapshotRebuildWorker` 内，是接口，用于承载 `Projection Checkpoint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionCheckpointStore` is an interface inside `RuntimeSnapshotRebuildWorker` and carries the responsibility, state, or contract for `Projection Checkpoint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionCheckpointStore` 作为 `RuntimeSnapshotRebuildWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionCheckpointStore` as the responsibility boundary of `RuntimeSnapshotRebuildWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ProjectionCheckpointStore {

        /**
         * 方法 `claim` 按照 `ProjectionCheckpointStore` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claim` processes its inputs according to `ProjectionCheckpointStore`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        Claim claim(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);

        /**
         * 方法 `complete` 按照 `ProjectionCheckpointStore` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `complete` processes its inputs according to `ProjectionCheckpointStore`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        void complete(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);

        /**
         * 方法 `release` 按照 `ProjectionCheckpointStore` 的职责处理输入，完成 `release` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `release` processes its inputs according to `ProjectionCheckpointStore`'s responsibility, performs the `release` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        void release(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);
    }

    /**
     * 类型 `RebuildPort` 位于 `RuntimeSnapshotRebuildWorker` 内，是接口，用于承载 `Rebuild Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RebuildPort` is an interface inside `RuntimeSnapshotRebuildWorker` and carries the responsibility, state, or contract for `Rebuild Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RebuildPort` 作为 `RuntimeSnapshotRebuildWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RebuildPort` as the responsibility boundary of `RuntimeSnapshotRebuildWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RebuildPort {

        /**
         * 方法 `rebuild` 按照 `RebuildPort` 的职责处理输入，完成 `rebuild` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rebuild` processes its inputs according to `RebuildPort`'s responsibility, performs the `rebuild` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rebuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rebuild`, then continue the business flow using its result, exception, or side effect.
         *
         * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rebuild(Rbac3RuntimeProjectionDeliveryHandler.EventEnvelope event);
    }
}
