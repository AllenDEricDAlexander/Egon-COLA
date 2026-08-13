package top.egon.cola.platform.rbac3.admin.session.domain.exception;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.session.service.AuthorizationContextFacade;

/**
     * 类型 `InactiveIdentityMembershipException` 位于 `AuthorizationContextFacade` 内，是类型，用于承载 `Inactive Identity Membership Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `InactiveIdentityMembershipException` is a type inside `AuthorizationContextFacade` and carries the responsibility, state, or contract for `Inactive Identity Membership Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `InactiveIdentityMembershipException` 作为 `AuthorizationContextFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `InactiveIdentityMembershipException` as the responsibility boundary of `AuthorizationContextFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public final class InactiveIdentityMembershipException
            extends IllegalStateException {

        /**
         * 构造器 `InactiveIdentityMembershipException` 用于创建并初始化 `InactiveIdentityMembershipException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `InactiveIdentityMembershipException` creates and initializes `InactiveIdentityMembershipException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `InactiveIdentityMembershipException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `InactiveIdentityMembershipException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public InactiveIdentityMembershipException(String tenantId, String identitySub) {
            super("active identity membership is required: tenantId="
                    + tenantId + ", identitySub=" + identitySub);
        }
    }
