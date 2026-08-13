package top.egon.cola.platform.rbac3.admin.session.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.ActiveMembershipVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.AuthorizationContextVO;
import top.egon.cola.platform.rbac3.admin.session.domain.exception.ConcurrentContextCreationException;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;

/**
     * 类型 `AuthorizationContextRepository` 位于 `AuthorizationContextFacade` 内，是接口，用于承载 `Authorization Context Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationContextRepository` is an interface inside `AuthorizationContextFacade` and carries the responsibility, state, or contract for `Authorization Context Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationContextRepository` 作为 `AuthorizationContextFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationContextRepository` as the responsibility boundary of `AuthorizationContextFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface AuthorizationContextRepository {

        /**
         * 方法 `find` 按照 `AuthorizationContextRepository` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `AuthorizationContextRepository`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<AuthorizationContextVO> find(String tenantId, String sessionId);

        /**
         * 方法 `create` 按照 `AuthorizationContextRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `AuthorizationContextRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param entityId 输入参数 `entityId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param membership 输入参数 `membership`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         * @throws ConcurrentContextCreationException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
         */
        AuthorizationContextVO create(
                long entityId,
                ActiveMembershipVO membership,
                String sessionId,
                Instant now,
                Instant expiresAt) throws ConcurrentContextCreationException;
    }
