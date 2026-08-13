package top.egon.cola.platform.rbac3.admin.runtime.repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.RetryResultVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeQueryService;

/**
     * 类型 `MutationRecoveryPort` 位于 `RuntimeQueryService` 内，是接口，用于承载 `Mutation Recovery Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationRecoveryPort` is an interface inside `RuntimeQueryService` and carries the responsibility, state, or contract for `Mutation Recovery Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationRecoveryPort` 作为 `RuntimeQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationRecoveryPort` as the responsibility boundary of `RuntimeQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface MutationRecoveryPort {
        /**
         * 方法 `retry` 按照 `MutationRecoveryPort` 的职责处理输入，完成 `retry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `retry` processes its inputs according to `MutationRecoveryPort`'s responsibility, performs the `retry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `retry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `retry`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RetryResultVO retry(String tenantId, String mutationId, String actorId);
    }
