package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.RuntimeSnapshotRebuildClaimEnum;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.RuntimeSnapshotRebuildWorker;

/**
     * 类型 `ProjectionCheckpointRepository` 位于 `RuntimeSnapshotRebuildWorker` 内，是接口，用于承载 `Projection Checkpoint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionCheckpointRepository` is an interface inside `RuntimeSnapshotRebuildWorker` and carries the responsibility, state, or contract for `Projection Checkpoint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionCheckpointRepository` 作为 `RuntimeSnapshotRebuildWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionCheckpointRepository` as the responsibility boundary of `RuntimeSnapshotRebuildWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ProjectionCheckpointRepository {

        /**
         * 方法 `claim` 按照 `ProjectionCheckpointRepository` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `claim` processes its inputs according to `ProjectionCheckpointRepository`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        RuntimeSnapshotRebuildClaimEnum claim(
                String tenantId,
                String eventId,
                String aggregateType,
                String aggregateId,
                long aggregateVersion);

        /**
         * 方法 `complete` 按照 `ProjectionCheckpointRepository` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `complete` processes its inputs according to `ProjectionCheckpointRepository`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
         * 方法 `release` 按照 `ProjectionCheckpointRepository` 的职责处理输入，完成 `release` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `release` processes its inputs according to `ProjectionCheckpointRepository`'s responsibility, performs the `release` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
