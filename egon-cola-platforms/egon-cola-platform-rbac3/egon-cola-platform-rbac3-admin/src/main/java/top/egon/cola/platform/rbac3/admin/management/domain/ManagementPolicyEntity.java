package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `ManagementPolicyEntity` 位于当前包内，是类型，用于承载 `Management Policy Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementPolicyEntity` is a type in its package and carries the responsibility, state, or contract for `Management Policy Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementPolicyEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementPolicyEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_management_policy")
public class ManagementPolicyEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ManagementPolicyEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `policyCode` 表示 `ManagementPolicyEntity` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "policy_code", nullable = false, length = 128)
    private String policyCode;
    /**
     * 字段 `name` 表示 `ManagementPolicyEntity` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `name` stores the `name`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `name` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `name`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 200)
    private String name;
    /**
     * 字段 `status` 表示 `ManagementPolicyEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `validFrom` 表示 `ManagementPolicyEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `ManagementPolicyEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;
    /**
     * 字段 `maximumAssignmentDays` 表示 `ManagementPolicyEntity` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;
    /**
     * 字段 `maximumRiskLevel` 表示 `ManagementPolicyEntity` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "max_risk_level", nullable = false, length = 32)
    private RiskLevel maximumRiskLevel;
    /**
     * 字段 `requiredAuthenticationStrength` 表示 `ManagementPolicyEntity` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `AuthenticationStrength`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `AuthenticationStrength`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_auth_strength", nullable = false, length = 32)
    private AuthenticationStrength requiredAuthenticationStrength;
    /**
     * 字段 `requireReason` 表示 `ManagementPolicyEntity` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_reason", nullable = false)
    private boolean requireReason;
    /**
     * 字段 `requireTicket` 表示 `ManagementPolicyEntity` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_ticket", nullable = false)
    private boolean requireTicket;
    /**
     * 字段 `includeInheritedSubjectRoles` 表示 `ManagementPolicyEntity` 中与 `include Inherited Subject Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `includeInheritedSubjectRoles` stores the `include Inherited Subject Roles`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "include_inherited_subject_roles", nullable = false)
    private boolean includeInheritedSubjectRoles;
    /**
     * 字段 `requireAllAffiliationsInScope` 表示 `ManagementPolicyEntity` 中与 `require All Affiliations In Scope` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In Scope`-related state, dependency, configuration, or result of `ManagementPolicyEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `ManagementPolicyEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `ManagementPolicyEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_all_affiliations_in_scope", nullable = false)
    private boolean requireAllAffiliationsInScope;

    /**
     * 构造器 `ManagementPolicyEntity` 用于创建并初始化 `ManagementPolicyEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyEntity` creates and initializes `ManagementPolicyEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementPolicyEntity() {
    }

    /**
     * 构造器 `ManagementPolicyEntity` 用于创建并初始化 `ManagementPolicyEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyEntity` creates and initializes `ManagementPolicyEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumRiskLevel 输入参数 `maximumRiskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requiredAuthenticationStrength 输入参数 `requiredAuthenticationStrength`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireReason 输入参数 `requireReason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireTicket 输入参数 `requireTicket`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param includeInheritedSubjectRoles 输入参数 `includeInheritedSubjectRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireAllAffiliationsInScope 输入参数 `requireAllAffiliationsInScope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementPolicyEntity(
            Long id,
            Long tenantId,
            String policyCode,
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            RiskLevel maximumRiskLevel,
            AuthenticationStrength requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            String actorId,
            Instant now
    ) {
        if (validTo != null && !validTo.isAfter(validFrom)
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("invalid management policy limits");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.policyCode = required(policyCode, "policyCode");
        this.name = required(name, "name");
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.maximumAssignmentDays = maximumAssignmentDays;
        this.maximumRiskLevel = Objects.requireNonNull(maximumRiskLevel, "maximumRiskLevel");
        this.requiredAuthenticationStrength = Objects.requireNonNull(
                requiredAuthenticationStrength, "requiredAuthenticationStrength");
        this.requireReason = requireReason;
        this.requireTicket = requireTicket;
        this.includeInheritedSubjectRoles = includeInheritedSubjectRoles;
        this.requireAllAffiliationsInScope = requireAllAffiliationsInScope;
        markCreated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getId() {
        return id;
    }

    /**
     * 方法 `getPolicyCode` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Policy Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPolicyCode` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Policy Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPolicyCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPolicyCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPolicyCode() {
        return policyCode;
    }

    /**
     * 方法 `getName` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getName() {
        return name;
    }

    /**
     * 方法 `getStatus` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 方法 `getValidFrom` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getValidFrom` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getValidFrom`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getValidFrom() {
        return validFrom;
    }

    /**
     * 方法 `getValidTo` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getValidTo` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getValidTo`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getValidTo() {
        return validTo;
    }

    /**
     * 方法 `getMaximumAssignmentDays` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Maximum Assignment Days` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumAssignmentDays` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Maximum Assignment Days` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMaximumAssignmentDays` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMaximumAssignmentDays`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Integer getMaximumAssignmentDays() {
        return maximumAssignmentDays;
    }

    /**
     * 方法 `getMaximumRiskLevel` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Maximum Risk Level` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumRiskLevel` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Maximum Risk Level` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMaximumRiskLevel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMaximumRiskLevel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RiskLevel getMaximumRiskLevel() {
        return maximumRiskLevel;
    }

    /**
     * 方法 `getRequiredAuthenticationStrength` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `get Required Authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRequiredAuthenticationStrength` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `get Required Authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRequiredAuthenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRequiredAuthenticationStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthenticationStrength getRequiredAuthenticationStrength() {
        return requiredAuthenticationStrength;
    }

    /**
     * 方法 `isRequireReason` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `is Require Reason` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireReason` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `is Require Reason` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isRequireReason` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isRequireReason`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isRequireReason() {
        return requireReason;
    }

    /**
     * 方法 `isRequireTicket` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `is Require Ticket` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireTicket` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `is Require Ticket` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isRequireTicket` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isRequireTicket`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isRequireTicket() {
        return requireTicket;
    }

    /**
     * 方法 `isIncludeInheritedSubjectRoles` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `is Include Inherited Subject Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isIncludeInheritedSubjectRoles` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `is Include Inherited Subject Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isIncludeInheritedSubjectRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isIncludeInheritedSubjectRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isIncludeInheritedSubjectRoles() {
        return includeInheritedSubjectRoles;
    }

    /**
     * 方法 `isRequireAllAffiliationsInScope` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `is Require All Affiliations In Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireAllAffiliationsInScope` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `is Require All Affiliations In Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isRequireAllAffiliationsInScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isRequireAllAffiliationsInScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isRequireAllAffiliationsInScope() {
        return requireAllAffiliationsInScope;
    }

    /**
     * 方法 `update` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumRiskLevel 输入参数 `maximumRiskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requiredAuthenticationStrength 输入参数 `requiredAuthenticationStrength`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireReason 输入参数 `requireReason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireTicket 输入参数 `requireTicket`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param includeInheritedSubjectRoles 输入参数 `includeInheritedSubjectRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requireAllAffiliationsInScope 输入参数 `requireAllAffiliationsInScope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            RiskLevel maximumRiskLevel,
            AuthenticationStrength requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            String actorId,
            Instant now
    ) {
        validate(validFrom, validTo, maximumAssignmentDays);
        this.name = required(name, "name");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.maximumAssignmentDays = maximumAssignmentDays;
        this.maximumRiskLevel = Objects.requireNonNull(maximumRiskLevel, "maximumRiskLevel");
        this.requiredAuthenticationStrength = Objects.requireNonNull(
                requiredAuthenticationStrength, "requiredAuthenticationStrength");
        this.requireReason = requireReason;
        this.requireTicket = requireTicket;
        this.includeInheritedSubjectRoles = includeInheritedSubjectRoles;
        this.requireAllAffiliationsInScope = requireAllAffiliationsInScope;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `disable` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `disable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `disable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void disable(String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("only active management policy can be disabled");
        }
        status = Status.DISABLED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `validate` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void validate(
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays
    ) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("invalid management policy limits");
        }
    }

    /**
     * 方法 `required` 按照 `ManagementPolicyEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ManagementPolicyEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `Status` 位于 `ManagementPolicyEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `ManagementPolicyEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `ManagementPolicyEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `ManagementPolicyEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `ACTIVE` 表示 `Status` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `DISABLED` 表示 `Status` 中与 `DISABLED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DISABLED` stores the `DISABLED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DISABLED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DISABLED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        DISABLED,
        /**
         * 字段 `EXPIRED` 表示 `Status` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED,
        /**
         * 字段 `ARCHIVED` 表示 `Status` 中与 `ARCHIVED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ARCHIVED` stores the `ARCHIVED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ARCHIVED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ARCHIVED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ARCHIVED
    }

    /**
     * 类型 `RiskLevel` 位于 `ManagementPolicyEntity` 内，是枚举，用于承载 `Risk Level` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RiskLevel` is an enum inside `ManagementPolicyEntity` and carries the responsibility, state, or contract for `Risk Level`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RiskLevel` 作为 `ManagementPolicyEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RiskLevel` as the responsibility boundary of `ManagementPolicyEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum RiskLevel {
        /**
         * 字段 `LOW` 表示 `RiskLevel` 中与 `LOW` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `LOW` stores the `LOW`-related state, dependency, configuration, or result of `RiskLevel` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `LOW` 时应保持 `RiskLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `LOW`, preserve `RiskLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        LOW,
        /**
         * 字段 `MEDIUM` 表示 `RiskLevel` 中与 `MEDIUM` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MEDIUM` stores the `MEDIUM`-related state, dependency, configuration, or result of `RiskLevel` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MEDIUM` 时应保持 `RiskLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MEDIUM`, preserve `RiskLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        MEDIUM,
        /**
         * 字段 `HIGH` 表示 `RiskLevel` 中与 `HIGH` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `HIGH` stores the `HIGH`-related state, dependency, configuration, or result of `RiskLevel` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `HIGH` 时应保持 `RiskLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `HIGH`, preserve `RiskLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        HIGH,
        /**
         * 字段 `CRITICAL` 表示 `RiskLevel` 中与 `CRITICAL` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CRITICAL` stores the `CRITICAL`-related state, dependency, configuration, or result of `RiskLevel` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CRITICAL` 时应保持 `RiskLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CRITICAL`, preserve `RiskLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        CRITICAL
    }

    /**
     * 类型 `AuthenticationStrength` 位于 `ManagementPolicyEntity` 内，是枚举，用于承载 `Authentication Strength` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthenticationStrength` is an enum inside `ManagementPolicyEntity` and carries the responsibility, state, or contract for `Authentication Strength`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthenticationStrength` 作为 `ManagementPolicyEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthenticationStrength` as the responsibility boundary of `ManagementPolicyEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum AuthenticationStrength {
        /**
         * 字段 `PASSWORD` 表示 `AuthenticationStrength` 中与 `PASSWORD` 相关的状态、依赖、配置或结果（声明类型 `AuthenticationStrength`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PASSWORD` stores the `PASSWORD`-related state, dependency, configuration, or result of `AuthenticationStrength` (declared type `AuthenticationStrength`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PASSWORD` 时应保持 `AuthenticationStrength` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PASSWORD`, preserve `AuthenticationStrength`'s lifecycle, immutability, and thread-safety constraints.
         */
        PASSWORD,
        /**
         * 字段 `MFA` 表示 `AuthenticationStrength` 中与 `MFA` 相关的状态、依赖、配置或结果（声明类型 `AuthenticationStrength`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MFA` stores the `MFA`-related state, dependency, configuration, or result of `AuthenticationStrength` (declared type `AuthenticationStrength`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MFA` 时应保持 `AuthenticationStrength` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MFA`, preserve `AuthenticationStrength`'s lifecycle, immutability, and thread-safety constraints.
         */
        MFA,
        /**
         * 字段 `STRONG` 表示 `AuthenticationStrength` 中与 `STRONG` 相关的状态、依赖、配置或结果（声明类型 `AuthenticationStrength`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `STRONG` stores the `STRONG`-related state, dependency, configuration, or result of `AuthenticationStrength` (declared type `AuthenticationStrength`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `STRONG` 时应保持 `AuthenticationStrength` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `STRONG`, preserve `AuthenticationStrength`'s lifecycle, immutability, and thread-safety constraints.
         */
        STRONG
    }
}
