package top.egon.cola.platform.rbac3.admin.runtime.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationFenceVO;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationFenceService;

/**
     * 类型 `AuthorizationFenceRepository` 位于 `AuthorizationFenceService` 内，是接口，用于承载 `AuthorizationFenceVO Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationFenceRepository` is an interface inside `AuthorizationFenceService` and carries the responsibility, state, or contract for `AuthorizationFenceVO Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationFenceRepository` 作为 `AuthorizationFenceService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationFenceRepository` as the responsibility boundary of `AuthorizationFenceService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface AuthorizationFenceRepository {
        /**
         * 方法 `put` 按照 `AuthorizationFenceRepository` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `put` processes its inputs according to `AuthorizationFenceRepository`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
         *
         * @param fence 输入参数 `fence`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void put(AuthorizationFenceVO fence);

        /**
         * 方法 `remove` 按照 `AuthorizationFenceRepository` 的职责处理输入，完成 `remove` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `remove` processes its inputs according to `AuthorizationFenceRepository`'s responsibility, performs the `remove` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `remove` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `remove`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void remove(String tenantId, String scopeType, String scopeId);
    }
