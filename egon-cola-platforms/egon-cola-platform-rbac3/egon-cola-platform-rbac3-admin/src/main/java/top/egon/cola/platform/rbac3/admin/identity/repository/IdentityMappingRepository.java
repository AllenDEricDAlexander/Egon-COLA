package top.egon.cola.platform.rbac3.admin.identity.repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;

/**
     * 类型 `IdentityMappingRepository` 位于 `IdentityMappingFacade` 内，是接口，用于承载 `MappingVO Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdentityMappingRepository` is an interface inside `IdentityMappingFacade` and carries the responsibility, state, or contract for `MappingVO Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdentityMappingRepository` 作为 `IdentityMappingFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdentityMappingRepository` as the responsibility boundary of `IdentityMappingFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface IdentityMappingRepository {

        /**
         * 方法 `find` 按照 `IdentityMappingRepository` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `IdentityMappingRepository`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<MappingVO> find(String tenantId, String identitySub);

        /**
         * 方法 `create` 按照 `IdentityMappingRepository` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `IdentityMappingRepository`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param mappingId 输入参数 `mappingId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rbac3UserId 输入参数 `rbac3UserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MappingVO create(
                long mappingId,
                String tenantId,
                String identitySub,
                String rbac3UserId,
                String actorId,
                Instant now);

        /**
         * 方法 `resolve` 按照 `IdentityMappingRepository` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `resolve` processes its inputs according to `IdentityMappingRepository`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `resolve` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `resolve`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<ResolvedMembershipVO> resolve(String tenantId, String identitySub);

        /**
         * 方法 `tenants` 按照 `IdentityMappingRepository` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `tenants` processes its inputs according to `IdentityMappingRepository`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<TenantMembershipVO> tenants(String identitySub);
    }
