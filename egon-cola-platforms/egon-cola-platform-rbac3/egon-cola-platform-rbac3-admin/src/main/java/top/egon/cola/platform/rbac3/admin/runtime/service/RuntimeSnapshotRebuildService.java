package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.controller.scheduled.RuntimeSnapshotRebuildWorker;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;

/**
     * 类型 `RuntimeSnapshotRebuildService` 位于 `RuntimeSnapshotRebuildWorker` 内，是接口，用于承载 `Rebuild Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeSnapshotRebuildService` is an interface inside `RuntimeSnapshotRebuildWorker` and carries the responsibility, state, or contract for `Rebuild Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeSnapshotRebuildService` 作为 `RuntimeSnapshotRebuildWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeSnapshotRebuildService` as the responsibility boundary of `RuntimeSnapshotRebuildWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RuntimeSnapshotRebuildService {

        /**
         * 方法 `rebuild` 按照 `RuntimeSnapshotRebuildService` 的职责处理输入，完成 `rebuild` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rebuild` processes its inputs according to `RuntimeSnapshotRebuildService`'s responsibility, performs the `rebuild` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rebuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rebuild`, then continue the business flow using its result, exception, or side effect.
         *
         * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rebuild(EventEnvelopeVO event);
    }
