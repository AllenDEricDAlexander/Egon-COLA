package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;

import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.RuntimeSnapshotRebuildClaimEnum;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ProjectionCheckpointRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeSnapshotRebuildService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;

/**
 * 类型 `RuntimeSnapshotRebuildWorker` 位于当前包内，是类型，用于承载 `Runtime Snapshot Rebuild Worker` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RuntimeSnapshotRebuildWorker` is a type in its package and carries the responsibility, state, or contract for `Runtime Snapshot Rebuild Worker`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Idempotently rebuilds runtime projections by stable event and aggregate version.
 */
public final class RuntimeSnapshotProjectionService
        implements RuntimeProjectionService {

    /**
     * 字段 `checkpoints` 表示 `RuntimeSnapshotRebuildWorker` 中与 `checkpoints` 相关的状态、依赖、配置或结果（声明类型 `ProjectionCheckpointRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `checkpoints` stores the `checkpoints`-related state, dependency, configuration, or result of `RuntimeSnapshotRebuildWorker` (declared type `ProjectionCheckpointRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `checkpoints` 时应保持 `RuntimeSnapshotRebuildWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `checkpoints`, preserve `RuntimeSnapshotRebuildWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ProjectionCheckpointRepository checkpoints;
    /**
     * 字段 `rebuildPort` 表示 `RuntimeSnapshotRebuildWorker` 中与 `rebuild Port` 相关的状态、依赖、配置或结果（声明类型 `RuntimeSnapshotRebuildService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `rebuildPort` stores the `rebuild Port`-related state, dependency, configuration, or result of `RuntimeSnapshotRebuildWorker` (declared type `RuntimeSnapshotRebuildService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `rebuildPort` 时应保持 `RuntimeSnapshotRebuildWorker` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `rebuildPort`, preserve `RuntimeSnapshotRebuildWorker`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeSnapshotRebuildService rebuildPort;

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
    public RuntimeSnapshotProjectionService(
            ProjectionCheckpointRepository checkpoints,
            RuntimeSnapshotRebuildService rebuildPort) {
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
    public Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum project(
            EventEnvelopeVO event) {
        RuntimeSnapshotRebuildClaimEnum claim = checkpoints.claim(
                event.tenantId(), event.eventId(), event.aggregateType(),
                event.aggregateId(), event.aggregateVersion());
        if (claim == RuntimeSnapshotRebuildClaimEnum.ALREADY_APPLIED) {
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.ALREADY_APPLIED;
        }
        if (claim == RuntimeSnapshotRebuildClaimEnum.STALE) {
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.PERMANENT_FAILURE;
        }
        if (claim == RuntimeSnapshotRebuildClaimEnum.BUSY) {
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.RETRYABLE_FAILURE;
        }
        try {
            rebuildPort.rebuild(event);
            checkpoints.complete(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.APPLIED;
        } catch (RuntimeException unavailable) {
            checkpoints.release(
                    event.tenantId(), event.eventId(), event.aggregateType(),
                    event.aggregateId(), event.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.RETRYABLE_FAILURE;
        }
    }



    }
