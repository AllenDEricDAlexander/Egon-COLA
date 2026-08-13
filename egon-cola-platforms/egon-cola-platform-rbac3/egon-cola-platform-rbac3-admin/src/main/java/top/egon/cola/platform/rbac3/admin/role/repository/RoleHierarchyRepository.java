package top.egon.cola.platform.rbac3.admin.role.repository;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;

/**
     * 类型 `RoleHierarchyRepository` 位于 `RoleFacade` 内，是接口，用于承载 `Hierarchy Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleHierarchyRepository` is an interface inside `RoleFacade` and carries the responsibility, state, or contract for `Hierarchy Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleHierarchyRepository` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleHierarchyRepository` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RoleHierarchyRepository {

        /**
         * 方法 `withGraphLock` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `with Graph Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withGraphLock` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `with Graph Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withGraphLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `withGraphLock`, then continue the business flow using its result, exception, or side effect.
         *
         * @param <T> 类型参数表示图锁回调结果的具体类型；type parameter representing the graph-lock callback result type.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action);

        /**
         * 方法 `addEdge` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `add Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `addEdge` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `add Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `addEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `addEdge`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void addEdge(String tenantId, String applicationId, RoleEdge edge);

        /**
         * 方法 `removeEdge` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `remove Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `removeEdge` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `remove Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `removeEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `removeEdge`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void removeEdge(String tenantId, String applicationId, RoleEdge edge);

        /**
         * 方法 `rebuildClosure` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `rebuild Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rebuildClosure` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `rebuild Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rebuildClosure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rebuildClosure`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rebuildClosure(String tenantId, String applicationId);

        /**
         * 方法 `assertRoleVersion` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `assert Role Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assertRoleVersion` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `assert Role Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assertRoleVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assertRoleVersion`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void assertRoleVersion(
                String tenantId,
                String roleId,
                long expectedRoleVersion) {
        }

        /**
         * 方法 `recordGraphMutation` 按照 `RoleHierarchyRepository` 的职责处理输入，完成 `record Graph Mutation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `recordGraphMutation` processes its inputs according to `RoleHierarchyRepository`'s responsibility, performs the `record Graph Mutation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `recordGraphMutation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `recordGraphMutation`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param added 输入参数 `added`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void recordGraphMutation(
                String tenantId,
                String applicationId,
                RoleEdge edge,
                boolean added,
                String actorId) {
        }
    }
