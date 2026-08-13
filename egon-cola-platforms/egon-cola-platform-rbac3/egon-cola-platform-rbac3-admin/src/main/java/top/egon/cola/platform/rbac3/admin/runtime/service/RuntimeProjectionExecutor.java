package top.egon.cola.platform.rbac3.admin.runtime.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;

/**
     * 类型 `RuntimeProjectionExecutor` 位于 `AuthorizationMutationRecoveryWorker` 内，是接口，用于承载 `Projection Executor` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeProjectionExecutor` is an interface inside `AuthorizationMutationRecoveryWorker` and carries the responsibility, state, or contract for `Projection Executor`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeProjectionExecutor` 作为 `AuthorizationMutationRecoveryWorker` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeProjectionExecutor` as the responsibility boundary of `AuthorizationMutationRecoveryWorker`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RuntimeProjectionExecutor {

        /**
         * 方法 `project` 按照 `RuntimeProjectionExecutor` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `project` processes its inputs according to `RuntimeProjectionExecutor`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mutation 输入参数 `mutation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void project(MutationWorkDTO mutation);
    }
