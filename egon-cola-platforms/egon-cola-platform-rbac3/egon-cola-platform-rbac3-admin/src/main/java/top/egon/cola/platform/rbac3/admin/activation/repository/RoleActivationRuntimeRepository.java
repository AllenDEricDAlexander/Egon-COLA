package top.egon.cola.platform.rbac3.admin.activation.repository;

import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.activation.ActiveRoleSetView;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesResult;
import top.egon.cola.platform.rbac3.core.activation.DefaultRoleActivationResolver;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationInput;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolver;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;

/**
     * 类型 `RoleActivationRuntimeRepository` 位于 `RoleActivationFacade` 内，是接口，用于承载 `Runtime Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleActivationRuntimeRepository` is an interface inside `RoleActivationFacade` and carries the responsibility, state, or contract for `Runtime Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleActivationRuntimeRepository` 作为 `RoleActivationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleActivationRuntimeRepository` as the responsibility boundary of `RoleActivationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RoleActivationRuntimeRepository {

        /**
         * 方法 `createFence` 按照 `RoleActivationRuntimeRepository` 的职责处理输入，完成 `create Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `createFence` processes its inputs according to `RoleActivationRuntimeRepository`'s responsibility, performs the `create Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `createFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `createFence`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void createFence(
                String tenantId,
                String sessionId,
                String mutationId,
                Duration ttl);

        /**
         * 方法 `publish` 按照 `RoleActivationRuntimeRepository` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `publish` processes its inputs according to `RoleActivationRuntimeRepository`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
         *
         * @param publication 输入参数 `publication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void publish(RuntimePublicationVO publication);
    }
