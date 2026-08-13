package top.egon.cola.platform.rbac3.admin.activation.repository;

import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.activation.service.ActiveRoleSetRevalidator;

/**
     * 类型 `ReselectionRepository` 位于 `ActiveRoleSetRevalidator` 内，是接口，用于承载 `Reselection Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ReselectionRepository` is an interface inside `ActiveRoleSetRevalidator` and carries the responsibility, state, or contract for `Reselection Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ReselectionRepository` 作为 `ActiveRoleSetRevalidator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ReselectionRepository` as the responsibility boundary of `ActiveRoleSetRevalidator`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ReselectionRepository {
        /**
         * 方法 `requireReselection` 按照 `ReselectionRepository` 的职责处理输入，完成 `require Reselection` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `requireReselection` processes its inputs according to `ReselectionRepository`'s responsibility, performs the `require Reselection` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `requireReselection` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `requireReselection`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedSessionVersion 输入参数 `expectedSessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void requireReselection(
                String tenantId,
                String sessionId,
                long expectedSessionVersion,
                Instant now,
                String actorId);
    }
