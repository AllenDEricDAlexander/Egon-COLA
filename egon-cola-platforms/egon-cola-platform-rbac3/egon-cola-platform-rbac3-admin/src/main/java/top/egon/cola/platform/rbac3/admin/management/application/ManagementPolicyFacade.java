package top.egon.cola.platform.rbac3.admin.management.application;

import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
     * 字段 `factSource` 表示 `ManagementPolicyFacade` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `PolicyFactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `ManagementPolicyFacade` (declared type `PolicyFactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `ManagementPolicyFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `ManagementPolicyFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final PolicyFactSource factSource;
    /**
     * 字段 `controlStore` 表示 `ManagementPolicyFacade` 中与 `control Store` 相关的状态、依赖、配置或结果（声明类型 `ControlStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `controlStore` stores the `control Store`-related state, dependency, configuration, or result of `ManagementPolicyFacade` (declared type `ControlStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `controlStore` 时应保持 `ManagementPolicyFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `controlStore`, preserve `ManagementPolicyFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ControlStore controlStore;

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
            PolicyFactSource factSource
    ) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService");
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.controlStore = factSource instanceof ControlStore store ? store : null;
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
    public String authorize(Request request) {
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
    public List<PolicyView> policies(String tenantId) {
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
    public PolicyView policy(String tenantId, String policyId) {
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
    public PolicyView save(SaveCommand command) {
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
    public PolicyView disable(
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
    public CapabilityView capabilities(
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
    public List<ManagedUserView> manageableUsers(
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
    public List<ManagedRoleView> manageableRoles(
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
     * 类型 `PolicyFactSource` 位于 `ManagementPolicyFacade` 内，是接口，用于承载 `Policy Fact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyFactSource` is an interface inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Policy Fact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyFactSource` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyFactSource` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface PolicyFactSource {
        /**
         * 方法 `policies` 按照 `PolicyFactSource` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `policies` processes its inputs according to `PolicyFactSource`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetUserId 输入参数 `targetUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ManagementPolicyDecisionService.ManagementPolicyFact> policies(
                String tenantId,
                String subjectId,
                String targetUserId,
                Instant databaseNow);
    }

    /**
     * 类型 `ControlStore` 位于 `ManagementPolicyFacade` 内，是接口，用于承载 `Control Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ControlStore` is an interface inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Control Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ControlStore` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ControlStore` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ControlStore {
        /**
         * 方法 `policies` 按照 `ControlStore` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `policies` processes its inputs according to `ControlStore`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<PolicyView> policies(String tenantId);

        /**
         * 方法 `policy` 按照 `ControlStore` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `policy` processes its inputs according to `ControlStore`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `policy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `policy`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PolicyView policy(String tenantId, String policyId);

        /**
         * 方法 `save` 按照 `ControlStore` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `save` processes its inputs according to `ControlStore`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PolicyView save(SaveCommand command);

        /**
         * 方法 `disable` 按照 `ControlStore` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `disable` processes its inputs according to `ControlStore`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        PolicyView disable(
                String tenantId,
                String policyId,
                long expectedVersion,
                String actorId);

        /**
         * 方法 `capabilities` 按照 `ControlStore` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `capabilities` processes its inputs according to `ControlStore`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `capabilities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `capabilities`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjectUserId 输入参数 `subjectUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        CapabilityView capabilities(
                String tenantId,
                String subjectUserId,
                Instant databaseNow);

        /**
         * 方法 `manageableUsers` 按照 `ControlStore` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `manageableUsers` processes its inputs according to `ControlStore`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        List<ManagedUserView> manageableUsers(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);

        /**
         * 方法 `manageableRoles` 按照 `ControlStore` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `manageableRoles` processes its inputs according to `ControlStore`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        List<ManagedRoleView> manageableRoles(
                String tenantId,
                String subjectUserId,
                String query,
                Instant databaseNow);
    }

    /**
     * 类型 `Request` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Request` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Request` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Request` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param subjectId 记录组件 `subjectId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjectId` carries constructor data whose meaning is defined by the record contract.
     * @param targetUserId 记录组件 `targetUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetUserId` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleId 记录组件 `activationRootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param operation 记录组件 `operation` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operation` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param roleRisk 记录组件 `roleRisk` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleRisk` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentDays 记录组件 `assignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param reasonPresent 记录组件 `reasonPresent` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonPresent` carries constructor data whose meaning is defined by the record contract.
     * @param ticketPresent 记录组件 `ticketPresent` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ticketPresent` carries constructor data whose meaning is defined by the record contract.
     * @param databaseNow 记录组件 `databaseNow` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `databaseNow` carries constructor data whose meaning is defined by the record contract.
     */
    public record Request(
            /**
             * 字段 `tenantId` 表示 `Request` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `subjectId` 表示 `Request` 中与 `subject Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjectId` stores the `subject Id`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjectId` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjectId`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String subjectId,
            /**
             * 字段 `targetUserId` 表示 `Request` 中与 `target User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetUserId` stores the `target User Id`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetUserId` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetUserId`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetUserId,
            /**
             * 字段 `activationRootRoleId` 表示 `Request` 中与 `activation Root Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleId` stores the `activation Root Role Id`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleId` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleId`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationRootRoleId,
            /**
             * 字段 `operation` 表示 `Request` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operation` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operation`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String operation,
            /**
             * 字段 `authenticationStrength` 表示 `Request` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationStrength,
            /**
             * 字段 `roleRisk` 表示 `Request` 中与 `role Risk` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleRisk` stores the `role Risk`-related state, dependency, configuration, or result of `Request` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleRisk` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleRisk`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleRisk,
            /**
             * 字段 `assignmentDays` 表示 `Request` 中与 `assignment Days` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentDays` stores the `assignment Days`-related state, dependency, configuration, or result of `Request` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentDays` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentDays`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            int assignmentDays,
            /**
             * 字段 `reasonPresent` 表示 `Request` 中与 `reason Present` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonPresent` stores the `reason Present`-related state, dependency, configuration, or result of `Request` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonPresent` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonPresent`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean reasonPresent,
            /**
             * 字段 `ticketPresent` 表示 `Request` 中与 `ticket Present` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ticketPresent` stores the `ticket Present`-related state, dependency, configuration, or result of `Request` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ticketPresent` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ticketPresent`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean ticketPresent,
            /**
             * 字段 `databaseNow` 表示 `Request` 中与 `database Now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `databaseNow` stores the `database Now`-related state, dependency, configuration, or result of `Request` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `databaseNow` 时应保持 `Request` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `databaseNow`, preserve `Request`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant databaseNow
    ) {
    }

    /**
     * 类型 `SaveCommand` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Save Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SaveCommand` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Save Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SaveCommand` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SaveCommand` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param restrictions 记录组件 `restrictions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `restrictions` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SaveCommand(
            /**
             * 字段 `tenantId` 表示 `SaveCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SaveCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `policyId` 表示 `SaveCommand` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `SaveCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyId,
            /**
             * 字段 `policyCode` 表示 `SaveCommand` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `SaveCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyCode,
            /**
             * 字段 `name` 表示 `SaveCommand` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `SaveCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `validFrom` 表示 `SaveCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SaveCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SaveCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SaveCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `restrictions` 表示 `SaveCommand` 中与 `restrictions` 相关的状态、依赖、配置或结果（声明类型 `Restrictions`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `restrictions` stores the `restrictions`-related state, dependency, configuration, or result of `SaveCommand` (declared type `Restrictions`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `restrictions` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `restrictions`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Restrictions restrictions,
            /**
             * 字段 `subjects` 表示 `SaveCommand` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Subject&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `SaveCommand` (declared type `List&lt;Subject&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Subject> subjects,
            /**
             * 字段 `scopes` 表示 `SaveCommand` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Scope&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `SaveCommand` (declared type `List&lt;Scope&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Scope> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `SaveCommand` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `SaveCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `SaveCommand` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `SaveCommand` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `expectedVersion` 表示 `SaveCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SaveCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `SaveCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `SaveCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `SaveCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `SaveCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
        /**
         * 构造器 `SaveCommand` 用于创建并初始化 `SaveCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SaveCommand` creates and initializes `SaveCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SaveCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SaveCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param restrictions 输入参数 `restrictions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SaveCommand {
            restrictions = Objects.requireNonNull(restrictions, "restrictions");
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
            if (subjects.isEmpty() || scopes.isEmpty()
                    || activationRootRoleIds.isEmpty() || operations.isEmpty()) {
                throw new IllegalArgumentException(
                        "management policy subjects, scopes, roles and operations are required");
            }
        }
    }

    /**
     * 类型 `PolicyView` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Policy View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyView` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Policy View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyView` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyView` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param restrictions 记录组件 `restrictions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `restrictions` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record PolicyView(
            /**
             * 字段 `policyId` 表示 `PolicyView` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `PolicyView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyId,
            /**
             * 字段 `policyCode` 表示 `PolicyView` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `PolicyView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String policyCode,
            /**
             * 字段 `name` 表示 `PolicyView` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `PolicyView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String name,
            /**
             * 字段 `status` 表示 `PolicyView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `PolicyView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `validFrom` 表示 `PolicyView` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `PolicyView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `PolicyView` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `PolicyView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `restrictions` 表示 `PolicyView` 中与 `restrictions` 相关的状态、依赖、配置或结果（声明类型 `Restrictions`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `restrictions` stores the `restrictions`-related state, dependency, configuration, or result of `PolicyView` (declared type `Restrictions`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `restrictions` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `restrictions`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Restrictions restrictions,
            /**
             * 字段 `subjects` 表示 `PolicyView` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Subject&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `PolicyView` (declared type `List&lt;Subject&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Subject> subjects,
            /**
             * 字段 `scopes` 表示 `PolicyView` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Scope&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `PolicyView` (declared type `List&lt;Scope&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<Scope> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `PolicyView` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `PolicyView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `PolicyView` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `PolicyView` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `version` 表示 `PolicyView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `PolicyView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `PolicyView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `PolicyView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
        /**
         * 构造器 `PolicyView` 用于创建并初始化 `PolicyView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyView` creates and initializes `PolicyView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param restrictions 输入参数 `restrictions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PolicyView {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
            operations = Set.copyOf(operations);
        }
    }

    /**
     * 类型 `Restrictions` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Restrictions` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Restrictions` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Restrictions`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Restrictions` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Restrictions` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param maximumRiskLevel 记录组件 `maximumRiskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumRiskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param requiredAuthenticationStrength 记录组件 `requiredAuthenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requiredAuthenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param requireReason 记录组件 `requireReason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireReason` carries constructor data whose meaning is defined by the record contract.
     * @param requireTicket 记录组件 `requireTicket` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireTicket` carries constructor data whose meaning is defined by the record contract.
     * @param includeInheritedSubjectRoles 记录组件 `includeInheritedSubjectRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `includeInheritedSubjectRoles` carries constructor data whose meaning is defined by the record contract.
     * @param requireAllAffiliationsInScope 记录组件 `requireAllAffiliationsInScope` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireAllAffiliationsInScope` carries constructor data whose meaning is defined by the record contract.
     */
    public record Restrictions(
            /**
             * 字段 `maximumAssignmentDays` 表示 `Restrictions` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `Restrictions` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `maximumRiskLevel` 表示 `Restrictions` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `Restrictions` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            String maximumRiskLevel,
            /**
             * 字段 `requiredAuthenticationStrength` 表示 `Restrictions` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `Restrictions` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requiredAuthenticationStrength,
            /**
             * 字段 `requireReason` 表示 `Restrictions` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireReason,
            /**
             * 字段 `requireTicket` 表示 `Restrictions` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireTicket,
            /**
             * 字段 `includeInheritedSubjectRoles` 表示 `Restrictions` 中与 `include Inherited Subject Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `includeInheritedSubjectRoles` stores the `include Inherited Subject Roles`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean includeInheritedSubjectRoles,
            /**
             * 字段 `requireAllAffiliationsInScope` 表示 `Restrictions` 中与 `require All Affiliations In Scope` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In Scope`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireAllAffiliationsInScope
    ) {
    }

    /**
     * 类型 `Subject` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Subject` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Subject` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Subject`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Subject` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Subject` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     */
    public record Subject(/**
 * 字段 `type` 表示 `Subject` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String type, /**
 * 字段 `id` 表示 `Subject` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ String id) {
    }

    /**
     * 类型 `Scope` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Scope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Scope` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Scope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Scope` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Scope` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Scope(/**
 * 字段 `type` 表示 `Scope` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ String type, /**
 * 字段 `referenceId` 表示 `Scope` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ String referenceId) {
    }

    /**
     * 类型 `CapabilityView` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Capability View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CapabilityView` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Capability View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CapabilityView` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CapabilityView` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param policyIds 记录组件 `policyIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     */
    public record CapabilityView(
            /**
             * 字段 `policyIds` 表示 `CapabilityView` 中与 `policy Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyIds` stores the `policy Ids`-related state, dependency, configuration, or result of `CapabilityView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyIds` 时应保持 `CapabilityView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyIds`, preserve `CapabilityView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> policyIds,
            /**
             * 字段 `operations` 表示 `CapabilityView` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `CapabilityView` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `CapabilityView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `CapabilityView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> operations,
            /**
             * 字段 `activationRootRoleIds` 表示 `CapabilityView` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `CapabilityView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `CapabilityView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `CapabilityView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRootRoleIds
    ) {
        /**
         * 构造器 `CapabilityView` 用于创建并初始化 `CapabilityView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `CapabilityView` creates and initializes `CapabilityView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `CapabilityView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `CapabilityView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param policyIds 输入参数 `policyIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public CapabilityView {
            policyIds = List.copyOf(policyIds);
            operations = Set.copyOf(operations);
            activationRootRoleIds = List.copyOf(activationRootRoleIds);
        }
    }

    /**
     * 类型 `ManagedUserView` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Managed User View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagedUserView` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Managed User View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagedUserView` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagedUserView` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param username 记录组件 `username` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `username` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagedUserView(
            /**
             * 字段 `userId` 表示 `ManagedUserView` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ManagedUserView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `ManagedUserView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `ManagedUserView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `username` 表示 `ManagedUserView` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `username` stores the `username`-related state, dependency, configuration, or result of `ManagedUserView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `username` 时应保持 `ManagedUserView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `username`, preserve `ManagedUserView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String username,
            /**
             * 字段 `displayName` 表示 `ManagedUserView` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `ManagedUserView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayName` 时应保持 `ManagedUserView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayName`, preserve `ManagedUserView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String displayName
    ) {
    }

    /**
     * 类型 `ManagedRoleView` 位于 `ManagementPolicyFacade` 内，是记录类型，用于承载 `Managed Role View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagedRoleView` is a record inside `ManagementPolicyFacade` and carries the responsibility, state, or contract for `Managed Role View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagedRoleView` 作为 `ManagementPolicyFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagedRoleView` as the responsibility boundary of `ManagementPolicyFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagedRoleView(
            /**
             * 字段 `roleId` 表示 `ManagedRoleView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `ManagedRoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `ManagedRoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `ManagedRoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `roleCode` 表示 `ManagedRoleView` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `ManagedRoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `ManagedRoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `ManagedRoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `roleName` 表示 `ManagedRoleView` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `ManagedRoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `ManagedRoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `ManagedRoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleName,
            /**
             * 字段 `riskLevel` 表示 `ManagedRoleView` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `ManagedRoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `ManagedRoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `ManagedRoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String riskLevel,
            /**
             * 字段 `privileged` 表示 `ManagedRoleView` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `ManagedRoleView` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `ManagedRoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `ManagedRoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged
    ) {
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
    private ControlStore store() {
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
