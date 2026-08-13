package top.egon.cola.platform.rbac3.admin.assignment.application;

import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.core.constraint.PrerequisiteRoleSpecification;
import top.egon.cola.platform.rbac3.core.constraint.RoleCardinalitySpecification;
import top.egon.cola.platform.rbac3.core.constraint.SsdSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 类型 `AssignmentFacade` 位于当前包内，是类型，用于承载 `Assignment Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AssignmentFacade` is a type in its package and carries the responsibility, state, or contract for `Assignment Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Orchestrates delegated role assignment in the fixed security-check order.
 */
public final class AssignmentFacade {

    /**
     * 字段 `managementPolicyFacade` 表示 `AssignmentFacade` 中与 `management Policy Facade` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `managementPolicyFacade` stores the `management Policy Facade`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `ManagementPolicyFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `managementPolicyFacade` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `managementPolicyFacade`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManagementPolicyFacade managementPolicyFacade;
    /**
     * 字段 `factSource` 表示 `AssignmentFacade` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `AssignmentFactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `AssignmentFactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AssignmentFactSource factSource;
    /**
     * 字段 `assignmentLock` 表示 `AssignmentFacade` 中与 `assignment Lock` 相关的状态、依赖、配置或结果（声明类型 `AssignmentLock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `assignmentLock` stores the `assignment Lock`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `AssignmentLock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `assignmentLock` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `assignmentLock`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AssignmentLock assignmentLock;
    /**
     * 字段 `assignmentStore` 表示 `AssignmentFacade` 中与 `assignment Store` 相关的状态、依赖、配置或结果（声明类型 `AssignmentStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `assignmentStore` stores the `assignment Store`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `AssignmentStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `assignmentStore` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `assignmentStore`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AssignmentStore assignmentStore;
    /**
     * 字段 `mutationCoordinator` 表示 `AssignmentFacade` 中与 `mutation Coordinator` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationCoordinator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mutationCoordinator` stores the `mutation Coordinator`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `AuthorizationMutationCoordinator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mutationCoordinator` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mutationCoordinator`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationMutationCoordinator mutationCoordinator;
    /**
     * 字段 `ssdSpecification` 表示 `AssignmentFacade` 中与 `ssd Specification` 相关的状态、依赖、配置或结果（声明类型 `SsdSpecification`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ssdSpecification` stores the `ssd Specification`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `SsdSpecification`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ssdSpecification` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ssdSpecification`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SsdSpecification ssdSpecification = new SsdSpecification();
    /**
     * 字段 `prerequisiteSpecification` 表示 `AssignmentFacade` 中与 `prerequisite Specification` 相关的状态、依赖、配置或结果（声明类型 `PrerequisiteRoleSpecification`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `prerequisiteSpecification` stores the `prerequisite Specification`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `PrerequisiteRoleSpecification`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `prerequisiteSpecification` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `prerequisiteSpecification`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final PrerequisiteRoleSpecification prerequisiteSpecification =
            new PrerequisiteRoleSpecification();
    /**
     * 字段 `cardinalitySpecification` 表示 `AssignmentFacade` 中与 `cardinality Specification` 相关的状态、依赖、配置或结果（声明类型 `RoleCardinalitySpecification`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cardinalitySpecification` stores the `cardinality Specification`-related state, dependency, configuration, or result of `AssignmentFacade` (declared type `RoleCardinalitySpecification`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cardinalitySpecification` 时应保持 `AssignmentFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cardinalitySpecification`, preserve `AssignmentFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleCardinalitySpecification cardinalitySpecification =
            new RoleCardinalitySpecification();

    /**
     * 构造器 `AssignmentFacade` 用于创建并初始化 `AssignmentFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AssignmentFacade` creates and initializes `AssignmentFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AssignmentFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AssignmentFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param managementPolicyFacade 输入参数 `managementPolicyFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentLock 输入参数 `assignmentLock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentStore 输入参数 `assignmentStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationCoordinator 输入参数 `mutationCoordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AssignmentFacade(
            ManagementPolicyFacade managementPolicyFacade,
            AssignmentFactSource factSource,
            AssignmentLock assignmentLock,
            AssignmentStore assignmentStore,
            AuthorizationMutationCoordinator mutationCoordinator
    ) {
        this.managementPolicyFacade = Objects.requireNonNull(
                managementPolicyFacade, "managementPolicyFacade");
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.assignmentLock = Objects.requireNonNull(assignmentLock, "assignmentLock");
        this.assignmentStore = Objects.requireNonNull(assignmentStore, "assignmentStore");
        this.mutationCoordinator = Objects.requireNonNull(
                mutationCoordinator, "mutationCoordinator");
    }

    /**
     * 方法 `assign` 按照 `AssignmentFacade` 的职责处理输入，完成 `assign` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assign` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `assign` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assign` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assign`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AssignmentResult assign(AssignRequest request) {
        AssignmentFacts initial = factSource.load(request);
        int assignmentDays = assignmentDays(request.validFrom(), request.validTo());
        String managementPolicyId = managementPolicyFacade.authorize(
                new ManagementPolicyFacade.Request(
                request.tenantId(), request.actorId(), request.targetUserId(),
                initial.activationRootRoleId(), operation(request.assignmentType()),
                request.authenticationStrength(), initial.roleRisk(), assignmentDays,
                hasText(request.reason()), hasText(request.ticketNo()), request.databaseNow()));
        if (initial.privileged() && !request.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        if (request.actorId().equals(request.targetUserId())) {
            throw new Rbac3RuleViolation("SELF_PRIVILEGE_ESCALATION_DENIED");
        }
        if (initial.maximumAssignmentDays() != null
                && assignmentDays > initial.maximumAssignmentDays()) {
            throw new Rbac3RuleViolation("MANAGEMENT_POLICY_DENIED");
        }
        validateRules(initial, request.roleId());

        Cardinality cardinality = initial.cardinality();
        @SuppressWarnings("unchecked")
        AuthorizationMutationCoordinator.MutationResult<String> mutation =
                (AuthorizationMutationCoordinator.MutationResult<String>)
                        assignmentLock.withLock(new LockExecution(
                                request.tenantId(), initial.activationRootRoleId(),
                                cardinality.scopeType(), cardinality.scopeId(), () -> {
                                    AssignmentFacts locked = factSource.load(request);
                                    validateRules(locked, request.roleId());
                                    return mutationCoordinator.execute(
                                            new AuthorizationMutationCoordinator.MutationScope(
                                                    request.tenantId(), "USER",
                                                    request.targetUserId(), request.commandId(),
                                                    request.actorId()),
                                            request.targetUserId(),
                                            new AuthorizationMutationCoordinator.ExpectedVersions(
                                                    null, null,
                                                    request.expectedUserAuthVersion(),
                                                    request.validFrom().isAfter(
                                                            request.databaseNow())
                                                            ? request.expectedUserAuthVersion()
                                                            : Math.incrementExact(request
                                                                    .expectedUserAuthVersion()),
                                                    null, null),
                                            () -> assignmentStore.assign(new AssignmentCommand(
                                                    request, initial.activationRootRoleId(),
                                                    managementPolicyId)));
                                }));
        return new AssignmentResult(
                mutation.value(), mutation.mutationId(), mutation.completed(),
                mutation.reasonCode(), mutation.versions().newAuthVersion());
    }

    /**
     * 方法 `assignments` 按照 `AssignmentFacade` 的职责处理输入，完成 `assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignments` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignments`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<AssignmentView> assignments(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        return List.copyOf(assignmentStore.assignments(
                tenantId, userId, databaseNow));
    }

    /**
     * 方法 `change` 按照 `AssignmentFacade` 的职责处理输入，完成 `change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `change` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `change` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `change`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AssignmentResult change(ChangeRequest request) {
        AssignmentChangeFacts facts = factSource.loadChange(request);
        String operation = request.operation().name() + "_ROLE";
        boolean permittedSelfRevoke = false;
        if (request.actorId().equals(request.targetUserId())
                && request.operation() == ChangeOperation.REVOKE
                && "LOW".equals(facts.roleRisk())) {
            operation = "SELF_REVOKE_LOW_RISK";
            permittedSelfRevoke = true;
        }
        managementPolicyFacade.authorize(new ManagementPolicyFacade.Request(
                request.tenantId(), request.actorId(), request.targetUserId(),
                facts.activationRootRoleId(), operation,
                request.authenticationStrength(), facts.roleRisk(), 1,
                hasText(request.reason()), hasText(request.ticketNo()),
                request.databaseNow()));
        if (facts.privileged() && !request.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        if (request.actorId().equals(request.targetUserId()) && !permittedSelfRevoke) {
            throw new Rbac3RuleViolation("SELF_PRIVILEGE_ESCALATION_DENIED");
        }
        @SuppressWarnings("unchecked")
        AuthorizationMutationCoordinator.MutationResult<String> mutation =
                (AuthorizationMutationCoordinator.MutationResult<String>)
                        assignmentLock.withLock(new LockExecution(
                                request.tenantId(), facts.activationRootRoleId(),
                                "TENANT", request.tenantId(),
                                () -> mutationCoordinator.execute(
                                        new AuthorizationMutationCoordinator.MutationScope(
                                                request.tenantId(), "USER",
                                                request.targetUserId(), request.commandId(),
                                                request.actorId()),
                                        request.targetUserId(),
                                        new AuthorizationMutationCoordinator.ExpectedVersions(
                                                null, null,
                                                request.expectedUserAuthVersion(),
                                                Math.incrementExact(
                                                        request.expectedUserAuthVersion()),
                                                null, null),
                                        () -> assignmentStore.change(request))));
        return new AssignmentResult(
                mutation.value(), mutation.mutationId(), mutation.completed(),
                mutation.reasonCode(), mutation.versions().newAuthVersion());
    }

    /**
     * 方法 `validateRules` 按照 `AssignmentFacade` 的职责处理输入，完成 `validate Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateRules` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `validate Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestedRoleId 输入参数 `requestedRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateRules(AssignmentFacts facts, String requestedRoleId) {
        Set<String> resulting = new HashSet<>(facts.currentRoleIds());
        resulting.add(requestedRoleId);
        require(ssdSpecification.evaluate(resulting, facts.ssdSets()));
        for (PrerequisiteRoleSpecification.PrerequisiteGroup group
                : facts.prerequisiteGroups()) {
            require(prerequisiteSpecification.evaluate(resulting, group));
        }
        require(cardinalitySpecification.evaluate(
                facts.cardinality().activeAssignments(),
                facts.cardinality().maximumActive()));
    }

    /**
     * 方法 `require` 按照 `AssignmentFacade` 的职责处理输入，完成 `require` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `require` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `require` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `require` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `require`, then continue the business flow using its result, exception, or side effect.
     *
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void require(RuleResult result) {
        if (!result.allowed()) {
            String reasonCode = "PREREQUISITE_ROLE_MISSING".equals(result.reasonCode())
                    ? "ROLE_PREREQUISITE_NOT_MET"
                    : result.reasonCode();
            throw new Rbac3RuleViolation(reasonCode, result.evidenceIds());
        }
    }

    /**
     * 方法 `assignmentDays` 按照 `AssignmentFacade` 的职责处理输入，完成 `assignment Days` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignmentDays` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `assignment Days` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignmentDays` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignmentDays`, then continue the business flow using its result, exception, or side effect.
     *
     * @param from 输入参数 `from`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param to 输入参数 `to`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private int assignmentDays(Instant from, Instant to) {
        if (to == null) {
            return Integer.MAX_VALUE;
        }
        long seconds = Duration.between(from, to).toSeconds();
        return Math.toIntExact(Math.max(1L, (seconds + 86_399L) / 86_400L));
    }

    /**
     * 方法 `operation` 按照 `AssignmentFacade` 的职责处理输入，完成 `operation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operation` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `operation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param assignmentType 输入参数 `assignmentType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String operation(String assignmentType) {
        return "TEMPORARY".equals(assignmentType)
                ? "TEMPORARY_ASSIGN"
                : "ASSIGN_ROLE";
    }

    /**
     * 方法 `hasText` 按照 `AssignmentFacade` 的职责处理输入，完成 `has Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasText` processes its inputs according to `AssignmentFacade`'s responsibility, performs the `has Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 类型 `AssignmentFactSource` 位于 `AssignmentFacade` 内，是接口，用于承载 `Assignment Fact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentFactSource` is an interface inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Fact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentFactSource` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentFactSource` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface AssignmentFactSource {
        /**
         * 方法 `load` 按照 `AssignmentFactSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `AssignmentFactSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AssignmentFacts load(AssignRequest request);

        /**
         * 方法 `loadChange` 按照 `AssignmentFactSource` 的职责处理输入，完成 `load Change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `loadChange` processes its inputs according to `AssignmentFactSource`'s responsibility, performs the `load Change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `loadChange` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `loadChange`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default AssignmentChangeFacts loadChange(ChangeRequest request) {
            throw new UnsupportedOperationException("assignment change facts are not configured");
        }
    }

    /**
     * 类型 `AssignmentLock` 位于 `AssignmentFacade` 内，是接口，用于承载 `Assignment Lock` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentLock` is an interface inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Lock`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentLock` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentLock` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface AssignmentLock {
        /**
         * 方法 `withLock` 按照 `AssignmentLock` 的职责处理输入，完成 `with Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withLock` processes its inputs according to `AssignmentLock`'s responsibility, performs the `with Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `withLock`, then continue the business flow using its result, exception, or side effect.
         *
         * @param scope 输入参数 `scope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Object withLock(LockExecution scope);
    }

    /**
     * 类型 `AssignmentStore` 位于 `AssignmentFacade` 内，是接口，用于承载 `Assignment Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentStore` is an interface inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentStore` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentStore` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface AssignmentStore {
        /**
         * 方法 `assign` 按照 `AssignmentStore` 的职责处理输入，完成 `assign` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assign` processes its inputs according to `AssignmentStore`'s responsibility, performs the `assign` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assign` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assign`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        String assign(AssignmentCommand command);

        /**
         * 方法 `assignments` 按照 `AssignmentStore` 的职责处理输入，完成 `assignments` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assignments` processes its inputs according to `AssignmentStore`'s responsibility, performs the `assignments` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assignments` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assignments`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default List<AssignmentView> assignments(
                String tenantId,
                String userId,
                Instant databaseNow
        ) {
            throw new UnsupportedOperationException("assignment query is not configured");
        }

        /**
         * 方法 `change` 按照 `AssignmentStore` 的职责处理输入，完成 `change` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `change` processes its inputs according to `AssignmentStore`'s responsibility, performs the `change` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `change` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `change`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default String change(ChangeRequest request) {
            throw new UnsupportedOperationException("assignment change is not configured");
        }
    }

    /**
     * 类型 `LockExecution` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Lock Execution` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `LockExecution` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Lock Execution`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `LockExecution` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `LockExecution` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleId 记录组件 `activationRootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param action 记录组件 `action` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `action` carries constructor data whose meaning is defined by the record contract.
     */
    public record LockExecution(
            /**
             * 字段 `tenantId` 表示 `LockExecution` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `LockExecution` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `LockExecution` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `LockExecution`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `activationRootRoleId` 表示 `LockExecution` 中与 `activation Root Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleId` stores the `activation Root Role Id`-related state, dependency, configuration, or result of `LockExecution` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleId` 时应保持 `LockExecution` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleId`, preserve `LockExecution`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationRootRoleId,
            /**
             * 字段 `scopeType` 表示 `LockExecution` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `LockExecution` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `LockExecution` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `LockExecution`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `LockExecution` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `LockExecution` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `LockExecution` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `LockExecution`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `action` 表示 `LockExecution` 中与 `action` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `action` stores the `action`-related state, dependency, configuration, or result of `LockExecution` (declared type `Supplier&lt;Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `action` 时应保持 `LockExecution` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `action`, preserve `LockExecution`'s lifecycle, immutability, and thread-safety constraints.
             */
            Supplier<Object> action
    ) {
    }

    /**
     * 类型 `AssignRequest` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assign Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignRequest` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assign Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignRequest` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignRequest` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetUserId 记录组件 `targetUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetUserId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentType 记录组件 `assignmentType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentType` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param ticketNo 记录组件 `ticketNo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ticketNo` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param platformAdministrator 记录组件 `platformAdministrator` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `platformAdministrator` carries constructor data whose meaning is defined by the record contract.
     * @param expectedUserAuthVersion 记录组件 `expectedUserAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedUserAuthVersion` carries constructor data whose meaning is defined by the record contract.
     * @param commandId 记录组件 `commandId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `commandId` carries constructor data whose meaning is defined by the record contract.
     * @param databaseNow 记录组件 `databaseNow` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `databaseNow` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignRequest(
            /**
             * 字段 `tenantId` 表示 `AssignRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `actorId` 表示 `AssignRequest` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetUserId` 表示 `AssignRequest` 中与 `target User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetUserId` stores the `target User Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetUserId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetUserId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetUserId,
            /**
             * 字段 `roleId` 表示 `AssignRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `assignmentType` 表示 `AssignRequest` 中与 `assignment Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentType` stores the `assignment Type`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentType` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentType`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentType,
            /**
             * 字段 `validFrom` 表示 `AssignRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `reason` 表示 `AssignRequest` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `ticketNo` 表示 `AssignRequest` 中与 `ticket No` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ticketNo` stores the `ticket No`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ticketNo` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ticketNo`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ticketNo,
            /**
             * 字段 `authenticationStrength` 表示 `AssignRequest` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationStrength,
            /**
             * 字段 `platformAdministrator` 表示 `AssignRequest` 中与 `platform Administrator` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `platformAdministrator` stores the `platform Administrator`-related state, dependency, configuration, or result of `AssignRequest` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `platformAdministrator` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `platformAdministrator`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean platformAdministrator,
            /**
             * 字段 `expectedUserAuthVersion` 表示 `AssignRequest` 中与 `expected User Auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedUserAuthVersion` stores the `expected User Auth Version`-related state, dependency, configuration, or result of `AssignRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedUserAuthVersion` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedUserAuthVersion`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedUserAuthVersion,
            /**
             * 字段 `commandId` 表示 `AssignRequest` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `AssignRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `commandId` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `commandId`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String commandId,
            /**
             * 字段 `databaseNow` 表示 `AssignRequest` 中与 `database Now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `databaseNow` stores the `database Now`-related state, dependency, configuration, or result of `AssignRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `databaseNow` 时应保持 `AssignRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `databaseNow`, preserve `AssignRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant databaseNow
    ) {
    }

    /**
     * 类型 `AssignmentCommand` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentCommand` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentCommand` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentCommand` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param request 记录组件 `request` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `request` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleId 记录组件 `activationRootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param managementPolicyId 记录组件 `managementPolicyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `managementPolicyId` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentCommand(
            /**
             * 字段 `request` 表示 `AssignmentCommand` 中与 `request` 相关的状态、依赖、配置或结果（声明类型 `AssignRequest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `request` stores the `request`-related state, dependency, configuration, or result of `AssignmentCommand` (declared type `AssignRequest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `request` 时应保持 `AssignmentCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `request`, preserve `AssignmentCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            AssignRequest request,
            /**
             * 字段 `activationRootRoleId` 表示 `AssignmentCommand` 中与 `activation Root Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleId` stores the `activation Root Role Id`-related state, dependency, configuration, or result of `AssignmentCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleId` 时应保持 `AssignmentCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleId`, preserve `AssignmentCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationRootRoleId,
            /**
             * 字段 `managementPolicyId` 表示 `AssignmentCommand` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AssignmentCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AssignmentCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AssignmentCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String managementPolicyId
    ) {
    }

    /**
     * 类型 `ChangeRequest` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Change Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ChangeRequest` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Change Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ChangeRequest` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ChangeRequest` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetUserId 记录组件 `targetUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetUserId` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentId 记录组件 `assignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param operation 记录组件 `operation` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operation` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     * @param ticketNo 记录组件 `ticketNo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ticketNo` carries constructor data whose meaning is defined by the record contract.
     * @param authenticationStrength 记录组件 `authenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param platformAdministrator 记录组件 `platformAdministrator` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `platformAdministrator` carries constructor data whose meaning is defined by the record contract.
     * @param expectedAssignmentVersion 记录组件 `expectedAssignmentVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedAssignmentVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedUserAuthVersion 记录组件 `expectedUserAuthVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedUserAuthVersion` carries constructor data whose meaning is defined by the record contract.
     * @param commandId 记录组件 `commandId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `commandId` carries constructor data whose meaning is defined by the record contract.
     * @param databaseNow 记录组件 `databaseNow` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `databaseNow` carries constructor data whose meaning is defined by the record contract.
     */
    public record ChangeRequest(
            /**
             * 字段 `tenantId` 表示 `ChangeRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `actorId` 表示 `ChangeRequest` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetUserId` 表示 `ChangeRequest` 中与 `target User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetUserId` stores the `target User Id`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetUserId` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetUserId`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetUserId,
            /**
             * 字段 `assignmentId` 表示 `ChangeRequest` 中与 `assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentId` stores the `assignment Id`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentId` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentId`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentId,
            /**
             * 字段 `operation` 表示 `ChangeRequest` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `ChangeOperation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `ChangeOperation`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operation` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operation`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            ChangeOperation operation,
            /**
             * 字段 `reason` 表示 `ChangeRequest` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason,
            /**
             * 字段 `ticketNo` 表示 `ChangeRequest` 中与 `ticket No` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ticketNo` stores the `ticket No`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ticketNo` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ticketNo`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ticketNo,
            /**
             * 字段 `authenticationStrength` 表示 `ChangeRequest` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String authenticationStrength,
            /**
             * 字段 `platformAdministrator` 表示 `ChangeRequest` 中与 `platform Administrator` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `platformAdministrator` stores the `platform Administrator`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `platformAdministrator` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `platformAdministrator`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean platformAdministrator,
            /**
             * 字段 `expectedAssignmentVersion` 表示 `ChangeRequest` 中与 `expected Assignment Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedAssignmentVersion` stores the `expected Assignment Version`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedAssignmentVersion` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedAssignmentVersion`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedAssignmentVersion,
            /**
             * 字段 `expectedUserAuthVersion` 表示 `ChangeRequest` 中与 `expected User Auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedUserAuthVersion` stores the `expected User Auth Version`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedUserAuthVersion` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedUserAuthVersion`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedUserAuthVersion,
            /**
             * 字段 `commandId` 表示 `ChangeRequest` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `commandId` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `commandId`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String commandId,
            /**
             * 字段 `databaseNow` 表示 `ChangeRequest` 中与 `database Now` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `databaseNow` stores the `database Now`-related state, dependency, configuration, or result of `ChangeRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `databaseNow` 时应保持 `ChangeRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `databaseNow`, preserve `ChangeRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant databaseNow
    ) {
    }

    /**
     * 类型 `AssignmentChangeFacts` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment Change Facts` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentChangeFacts` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Change Facts`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentChangeFacts` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentChangeFacts` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param activationRootRoleId 记录组件 `activationRootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param roleRisk 记录组件 `roleRisk` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleRisk` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentChangeFacts(
            /**
             * 字段 `activationRootRoleId` 表示 `AssignmentChangeFacts` 中与 `activation Root Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleId` stores the `activation Root Role Id`-related state, dependency, configuration, or result of `AssignmentChangeFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleId` 时应保持 `AssignmentChangeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleId`, preserve `AssignmentChangeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationRootRoleId,
            /**
             * 字段 `roleRisk` 表示 `AssignmentChangeFacts` 中与 `role Risk` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleRisk` stores the `role Risk`-related state, dependency, configuration, or result of `AssignmentChangeFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleRisk` 时应保持 `AssignmentChangeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleRisk`, preserve `AssignmentChangeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleRisk,
            /**
             * 字段 `privileged` 表示 `AssignmentChangeFacts` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `AssignmentChangeFacts` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `AssignmentChangeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `AssignmentChangeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged
    ) {
    }

    /**
     * 类型 `AssignmentFacts` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment Facts` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentFacts` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Facts`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentFacts` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentFacts` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param activationRootRoleId 记录组件 `activationRootRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param roleRisk 记录组件 `roleRisk` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleRisk` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param currentRoleIds 记录组件 `currentRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `currentRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param ssdSets 记录组件 `ssdSets` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ssdSets` carries constructor data whose meaning is defined by the record contract.
     * @param prerequisiteGroups 记录组件 `prerequisiteGroups` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `prerequisiteGroups` carries constructor data whose meaning is defined by the record contract.
     * @param cardinality 记录组件 `cardinality` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `cardinality` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentFacts(
            /**
             * 字段 `activationRootRoleId` 表示 `AssignmentFacts` 中与 `activation Root Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleId` stores the `activation Root Role Id`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleId` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleId`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String activationRootRoleId,
            /**
             * 字段 `roleRisk` 表示 `AssignmentFacts` 中与 `role Risk` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleRisk` stores the `role Risk`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleRisk` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleRisk`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleRisk,
            /**
             * 字段 `privileged` 表示 `AssignmentFacts` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `roleType` 表示 `AssignmentFacts` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleType,
            /**
             * 字段 `maximumAssignmentDays` 表示 `AssignmentFacts` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `currentRoleIds` 表示 `AssignmentFacts` 中与 `current Role Ids` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `currentRoleIds` stores the `current Role Ids`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `currentRoleIds` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `currentRoleIds`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> currentRoleIds,
            /**
             * 字段 `ssdSets` 表示 `AssignmentFacts` 中与 `ssd Sets` 相关的状态、依赖、配置或结果（声明类型 `List&lt;SsdSpecification.SsdSet&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ssdSets` stores the `ssd Sets`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `List&lt;SsdSpecification.SsdSet&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ssdSets` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ssdSets`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<SsdSpecification.SsdSet> ssdSets,
            /**
             * 字段 `prerequisiteGroups` 表示 `AssignmentFacts` 中与 `prerequisite Groups` 相关的状态、依赖、配置或结果（声明类型 `List&lt;PrerequisiteRoleSpecification.PrerequisiteGroup&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `prerequisiteGroups` stores the `prerequisite Groups`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `List&lt;PrerequisiteRoleSpecification.PrerequisiteGroup&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `prerequisiteGroups` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `prerequisiteGroups`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<PrerequisiteRoleSpecification.PrerequisiteGroup> prerequisiteGroups,
            /**
             * 字段 `cardinality` 表示 `AssignmentFacts` 中与 `cardinality` 相关的状态、依赖、配置或结果（声明类型 `Cardinality`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `cardinality` stores the `cardinality`-related state, dependency, configuration, or result of `AssignmentFacts` (declared type `Cardinality`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `cardinality` 时应保持 `AssignmentFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `cardinality`, preserve `AssignmentFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            Cardinality cardinality
    ) {
        /**
         * 构造器 `AssignmentFacts` 用于创建并初始化 `AssignmentFacts` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AssignmentFacts` creates and initializes `AssignmentFacts`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AssignmentFacts` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AssignmentFacts`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param activationRootRoleId 输入参数 `activationRootRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleRisk 输入参数 `roleRisk`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param privileged 输入参数 `privileged`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleType 输入参数 `roleType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param currentRoleIds 输入参数 `currentRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ssdSets 输入参数 `ssdSets`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param prerequisiteGroups 输入参数 `prerequisiteGroups`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cardinality 输入参数 `cardinality`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AssignmentFacts {
            currentRoleIds = Set.copyOf(currentRoleIds);
            ssdSets = List.copyOf(ssdSets);
            prerequisiteGroups = List.copyOf(prerequisiteGroups);
        }
    }

    /**
     * 类型 `Cardinality` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Cardinality` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Cardinality` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Cardinality`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Cardinality` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Cardinality` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActive 记录组件 `maximumActive` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActive` carries constructor data whose meaning is defined by the record contract.
     * @param activeAssignments 记录组件 `activeAssignments` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activeAssignments` carries constructor data whose meaning is defined by the record contract.
     */
    public record Cardinality(
            /**
             * 字段 `scopeType` 表示 `Cardinality` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `Cardinality` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `Cardinality` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `Cardinality`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `Cardinality` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `Cardinality` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `Cardinality` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `Cardinality`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `maximumActive` 表示 `Cardinality` 中与 `maximum Active` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActive` stores the `maximum Active`-related state, dependency, configuration, or result of `Cardinality` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActive` 时应保持 `Cardinality` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActive`, preserve `Cardinality`'s lifecycle, immutability, and thread-safety constraints.
             */
            long maximumActive,
            /**
             * 字段 `activeAssignments` 表示 `Cardinality` 中与 `active Assignments` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activeAssignments` stores the `active Assignments`-related state, dependency, configuration, or result of `Cardinality` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activeAssignments` 时应保持 `Cardinality` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activeAssignments`, preserve `Cardinality`'s lifecycle, immutability, and thread-safety constraints.
             */
            long activeAssignments
    ) {
    }

    /**
     * 类型 `AssignmentResult` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentResult` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentResult` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentResult` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param assignmentId 记录组件 `assignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param mutationId 记录组件 `mutationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mutationId` carries constructor data whose meaning is defined by the record contract.
     * @param completed 记录组件 `completed` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `completed` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentResult(
            /**
             * 字段 `assignmentId` 表示 `AssignmentResult` 中与 `assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentId` stores the `assignment Id`-related state, dependency, configuration, or result of `AssignmentResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentId` 时应保持 `AssignmentResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentId`, preserve `AssignmentResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentId,
            /**
             * 字段 `mutationId` 表示 `AssignmentResult` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `AssignmentResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `AssignmentResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `AssignmentResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String mutationId,
            /**
             * 字段 `completed` 表示 `AssignmentResult` 中与 `completed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `completed` stores the `completed`-related state, dependency, configuration, or result of `AssignmentResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `completed` 时应保持 `AssignmentResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `completed`, preserve `AssignmentResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean completed,
            /**
             * 字段 `reasonCode` 表示 `AssignmentResult` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AssignmentResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AssignmentResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AssignmentResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `authVersion` 表示 `AssignmentResult` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `AssignmentResult` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `AssignmentResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `AssignmentResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long authVersion
    ) {
    }

    /**
     * 类型 `AssignmentView` 位于 `AssignmentFacade` 内，是记录类型，用于承载 `Assignment View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentView` is a record inside `AssignmentFacade` and carries the responsibility, state, or contract for `Assignment View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentView` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentView` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param assignmentId 记录组件 `assignmentId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param assignmentType 记录组件 `assignmentType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignmentType` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param sourceType 记录组件 `sourceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sourceType` carries constructor data whose meaning is defined by the record contract.
     * @param sourceId 记录组件 `sourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sourceId` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignmentView(
            /**
             * 字段 `assignmentId` 表示 `AssignmentView` 中与 `assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentId` stores the `assignment Id`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentId` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentId`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentId,
            /**
             * 字段 `roleId` 表示 `AssignmentView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `assignmentType` 表示 `AssignmentView` 中与 `assignment Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignmentType` stores the `assignment Type`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignmentType` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignmentType`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String assignmentType,
            /**
             * 字段 `status` 表示 `AssignmentView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `validFrom` 表示 `AssignmentView` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignmentView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignmentView` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignmentView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `sourceType` 表示 `AssignmentView` 中与 `source Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sourceType` stores the `source Type`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sourceType` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sourceType`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sourceType,
            /**
             * 字段 `sourceId` 表示 `AssignmentView` 中与 `source Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sourceId` stores the `source Id`-related state, dependency, configuration, or result of `AssignmentView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sourceId` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sourceId`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sourceId,
            /**
             * 字段 `version` 表示 `AssignmentView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `AssignmentView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `AssignmentView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `AssignmentView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }

    /**
     * 类型 `ChangeOperation` 位于 `AssignmentFacade` 内，是枚举，用于承载 `Change Operation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ChangeOperation` is an enum inside `AssignmentFacade` and carries the responsibility, state, or contract for `Change Operation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ChangeOperation` 作为 `AssignmentFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ChangeOperation` as the responsibility boundary of `AssignmentFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ChangeOperation {
        /**
         * 字段 `REVOKE` 表示 `ChangeOperation` 中与 `REVOKE` 相关的状态、依赖、配置或结果（声明类型 `ChangeOperation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKE` stores the `REVOKE`-related state, dependency, configuration, or result of `ChangeOperation` (declared type `ChangeOperation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKE` 时应保持 `ChangeOperation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKE`, preserve `ChangeOperation`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKE,
        /**
         * 字段 `SUSPEND` 表示 `ChangeOperation` 中与 `SUSPEND` 相关的状态、依赖、配置或结果（声明类型 `ChangeOperation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SUSPEND` stores the `SUSPEND`-related state, dependency, configuration, or result of `ChangeOperation` (declared type `ChangeOperation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SUSPEND` 时应保持 `ChangeOperation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SUSPEND`, preserve `ChangeOperation`'s lifecycle, immutability, and thread-safety constraints.
         */
        SUSPEND,
        /**
         * 字段 `RESUME` 表示 `ChangeOperation` 中与 `RESUME` 相关的状态、依赖、配置或结果（声明类型 `ChangeOperation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `RESUME` stores the `RESUME`-related state, dependency, configuration, or result of `ChangeOperation` (declared type `ChangeOperation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `RESUME` 时应保持 `ChangeOperation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `RESUME`, preserve `ChangeOperation`'s lifecycle, immutability, and thread-safety constraints.
         */
        RESUME
    }
}
