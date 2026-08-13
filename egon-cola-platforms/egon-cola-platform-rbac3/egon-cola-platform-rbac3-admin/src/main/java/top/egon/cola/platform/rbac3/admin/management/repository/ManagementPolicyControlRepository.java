package top.egon.cola.platform.rbac3.admin.management.repository;

import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.SaveCommandDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.PolicyVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.CapabilityVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedUserVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedRoleVO;
import top.egon.cola.platform.rbac3.admin.management.service.ManagementPolicyFacade;

/**
     * 类型 `ManagementPolicyControlRepository` 位于 `ManagementPolicyFacade` 内，是接口，用于承载 `Control Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagementPolicyControlRepository` is an interface inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Control Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagementPolicyControlRepository` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagementPolicyControlRepository` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ManagementPolicyControlRepository {
        /**
         * 方法 `policies` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `policies` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<PolicyVO> policies(String tenantId);

        /**
         * 方法 `policy` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `policy` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `policy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `policy`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PolicyVO policy(String tenantId, String policyId);

        /**
         * 方法 `save` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `save` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PolicyVO save(SaveCommandDTO command);

        /**
         * 方法 `disable` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `disable` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `disable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `disable`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PolicyVO disable(
                String tenantId,
                String policyId,
                long expectedVersion,
                String actorId);

        /**
         * 方法 `capabilities` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `capabilities` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `capabilities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `capabilities`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        CapabilityVO capabilities(
                String tenantId,
                String subjectUserId,
                Instant databaseNow);

        /**
         * 方法 `manageableUsers` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `manageableUsers` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `manageableUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `manageableUsers`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ManagedUserVO> manageableUsers(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);

        /**
         * 方法 `manageableRoles` 按照 `ManagementPolicyControlRepository` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `manageableRoles` processes its inputs according to `ManagementPolicyControlRepository`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `manageableRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `manageableRoles`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ManagedRoleVO> manageableRoles(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);
    }
