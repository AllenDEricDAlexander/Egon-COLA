package top.egon.cola.platform.rbac3.admin.session.domain.exception;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;

/**
     * 类型 `AuthorizationContextMismatchException` 位于 `AuthorizationContextFacade` 内，是类型，用于承载 `Authorization Context Mismatch Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationContextMismatchException` is a type inside `AuthorizationContextFacade` and carries the responsibility, state, or contract for `Authorization Context Mismatch Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationContextMismatchException` 作为 `AuthorizationContextFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationContextMismatchException` as the responsibility boundary of `AuthorizationContextFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public final class AuthorizationContextMismatchException
            extends IllegalStateException {

        /**
         * 构造器 `AuthorizationContextMismatchException` 用于创建并初始化 `AuthorizationContextMismatchException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationContextMismatchException` creates and initializes `AuthorizationContextMismatchException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationContextMismatchException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationContextMismatchException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationContextMismatchException(
                String tenantId, String sessionId, String identitySub) {
            super("authorization context does not match IdP identity: tenantId="
                    + tenantId + ", sessionId=" + sessionId
                    + ", identitySub=" + identitySub);
        }
    }
