package top.egon.cola.platform.rbac3.admin.management.service;

import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyFactRepository;
import top.egon.cola.platform.rbac3.admin.management.repository.ManagementPolicyControlRepository;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyRequestDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.SaveCommandDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.PolicyVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyRestrictionsVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicySubjectVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyScopeVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.CapabilityVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedUserVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedRoleVO;

/**
 * 类型 `ManagementPolicyFacade` 位于当前包内，是类型，用于承载 `Management Policy Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementPolicyFacade` is a type in its package and carries the responsibility, state, or contract for `Management Policy Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Requires one complete management policy to authorize one operation.
 */
public final class ManagementPolicyFacade {

    /**
     * 字段 `decisionService` 表示 `ManagementPolicyFacade` 中与 `decision Service` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyDecisionService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `decisionService` stores the `decision Service`-related state, dependency, configuration, or result of `ManagementPolicyFacade` (declared type `ManagementPolicyDecisionService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `decisionService` 时应保持 `ManagementPolicyFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `decisionService`, preserve `ManagementPolicyFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManagementPolicyDecisionService decisionService;
    /**
     * 字段 `factSource` 表示 `ManagementPolicyFacade` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `ManagementPolicyFacade` (declared type `ManagementPolicyFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `ManagementPolicyFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `ManagementPolicyFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManagementPolicyFactRepository factSource;
    /**
     * 字段 `controlStore` 表示 `ManagementPolicyFacade` 中与 `control Store` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyControlRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `controlStore` stores the `control Store`-related state, dependency, configuration, or result of `ManagementPolicyFacade` (declared type `ManagementPolicyControlRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `controlStore` 时应保持 `ManagementPolicyFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `controlStore`, preserve `ManagementPolicyFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManagementPolicyControlRepository controlStore;

    /**
     * 构造器 `ManagementPolicyFacade` 用于创建并初始化 `ManagementPolicyFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyFacade` creates and initializes `ManagementPolicyFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param decisionService 输入参数 `decisionService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementPolicyFacade(
            ManagementPolicyDecisionService decisionService,
            ManagementPolicyFactRepository factSource
    ) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService");
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.controlStore = factSource instanceof ManagementPolicyControlRepository store ? store : null;
    }

    /**
     * 方法 `authorize` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `authorize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorize` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `authorize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String authorize(ManagementPolicyRequestDTO request) {
        var decision = decisionService.decide(
                new ManagementPolicyDecisionService.ManagementDecisionInput(
                        request.subjectId(), request.targetUserId(),
                        request.activationRootRoleId(), request.operation(),
                        request.authenticationStrength(), request.roleRisk(),
                        request.assignmentDays(), request.reasonPresent(),
                        request.ticketPresent(), request.databaseNow(),
                        factSource.policies(
                                request.tenantId(), request.subjectId(),
                                request.targetUserId(), request.databaseNow())));
        if (!decision.allowed()) {
            throw new Rbac3RuleViolation(decision.reasonCode());
        }
        return decision.policyId();
    }

    /**
     * 方法 `policies` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<PolicyVO> policies(String tenantId) {
        return List.copyOf(store().policies(required(tenantId, "tenantId")));
    }

    /**
     * 方法 `policy` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policy` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public PolicyVO policy(String tenantId, String policyId) {
        return store().policy(
                required(tenantId, "tenantId"), required(policyId, "policyId"));
    }

    /**
     * 方法 `save` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public PolicyVO save(SaveCommandDTO command) {
        return store().save(Objects.requireNonNull(command, "command"));
    }

    /**
     * 方法 `disable` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public PolicyVO disable(
            String tenantId,
            String policyId,
            long expectedVersion,
            String actorId
    ) {
        return store().disable(
                required(tenantId, "tenantId"), required(policyId, "policyId"),
                expectedVersion, required(actorId, "actorId"));
    }

    /**
     * 方法 `capabilities` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `capabilities` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `capabilities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `capabilities`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public CapabilityVO capabilities(
            String tenantId,
            String subjectUserId,
            Instant databaseNow
    ) {
        return store().capabilities(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"),
                Objects.requireNonNull(databaseNow, "databaseNow"));
    }

    /**
     * 方法 `manageableUsers` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableUsers` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public List<ManagedUserVO> manageableUsers(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        return List.copyOf(store().manageableUsers(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"), query,
                Objects.requireNonNull(databaseNow, "databaseNow")));
    }

    /**
     * 方法 `manageableRoles` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableRoles` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
    public List<ManagedRoleVO> manageableRoles(
            String tenantId,
            String subjectUserId,
            String query,
            Instant databaseNow
    ) {
        return List.copyOf(store().manageableRoles(
                required(tenantId, "tenantId"),
                required(subjectUserId, "subjectUserId"), query,
                Objects.requireNonNull(databaseNow, "databaseNow")));
    }












    /**
     * 方法 `store` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `store` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `store` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `store` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `store` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `store`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ManagementPolicyControlRepository store() {
        if (controlStore == null) {
            throw new IllegalStateException("management policy control store is not configured");
        }
        return controlStore;
    }

    /**
     * 方法 `required` 按照 `ManagementPolicyFacade` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ManagementPolicyFacade`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
