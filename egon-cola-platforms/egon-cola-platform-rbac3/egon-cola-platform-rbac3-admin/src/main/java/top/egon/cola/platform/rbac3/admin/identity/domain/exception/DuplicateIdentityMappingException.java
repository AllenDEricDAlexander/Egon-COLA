package top.egon.cola.platform.rbac3.admin.identity.domain.exception;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;

/**
     * 类型 `DuplicateIdentityMappingException` 位于 `IdentityMappingFacade` 内，是类型，用于承载 `Duplicate Identity MappingVO Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DuplicateIdentityMappingException` is a type inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `Duplicate Identity MappingVO Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DuplicateIdentityMappingException` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DuplicateIdentityMappingException` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public final class DuplicateIdentityMappingException
            extends IllegalStateException {

        /**
         * 构造器 `DuplicateIdentityMappingException` 用于创建并初始化 `DuplicateIdentityMappingException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DuplicateIdentityMappingException` creates and initializes `DuplicateIdentityMappingException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DuplicateIdentityMappingException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DuplicateIdentityMappingException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param existingUserId 输入参数 `existingUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DuplicateIdentityMappingException(
                String tenantId, String identitySub, String existingUserId) {
            super("identity already maps to another tenant user: tenantId="
                    + tenantId + ", identitySub=" + identitySub
                    + ", rbac3UserId=" + existingUserId);
        }
    }
