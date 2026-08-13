package top.egon.cola.platform.rbac3.admin.management.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyStatusEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementPolicyAuthenticationStrengthEnum;

/**
 * 类型 `ManagementPolicyPO` 位于当前包内，是类型，用于承载 `Management Policy Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementPolicyPO` is a type in its package and carries the responsibility, state, or contract for `Management Policy Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementPolicyPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementPolicyPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ManagementPolicyEntity")
@Table(name = "rbac3_management_policy")
public class ManagementPolicyPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ManagementPolicyPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `policyCode` 表示 `ManagementPolicyPO` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "policy_code", nullable = false, length = 128)
    private String policyCode;
    /**
     * 字段 `name` 表示 `ManagementPolicyPO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `name` stores the `name`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `name` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `name`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 200)
    private String name;
    /**
     * 字段 `status` 表示 `ManagementPolicyPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `ManagementPolicyStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ManagementPolicyStatusEnum status;
    /**
     * 字段 `validFrom` 表示 `ManagementPolicyPO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `ManagementPolicyPO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;
    /**
     * 字段 `maximumAssignmentDays` 表示 `ManagementPolicyPO` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;
    /**
     * 字段 `maximumRiskLevel` 表示 `ManagementPolicyPO` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyRiskLevelEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `ManagementPolicyRiskLevelEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "max_risk_level", nullable = false, length = 32)
    private ManagementPolicyRiskLevelEnum maximumRiskLevel;
    /**
     * 字段 `requiredAuthenticationStrength` 表示 `ManagementPolicyPO` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyAuthenticationStrengthEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `ManagementPolicyAuthenticationStrengthEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_auth_strength", nullable = false, length = 32)
    private ManagementPolicyAuthenticationStrengthEnum requiredAuthenticationStrength;
    /**
     * 字段 `requireReason` 表示 `ManagementPolicyPO` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_reason", nullable = false)
    private boolean requireReason;
    /**
     * 字段 `requireTicket` 表示 `ManagementPolicyPO` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_ticket", nullable = false)
    private boolean requireTicket;
    /**
     * 字段 `includeInheritedSubjectRoles` 表示 `ManagementPolicyPO` 中与 `include Inherited Subject Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `includeInheritedSubjectRoles` stores the `include Inherited Subject Roles`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "include_inherited_subject_roles", nullable = false)
    private boolean includeInheritedSubjectRoles;
    /**
     * 字段 `requireAllAffiliationsInScope` 表示 `ManagementPolicyPO` 中与 `require All Affiliations In Scope` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In Scope`-related state, dependency, configuration, or result of `ManagementPolicyPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `ManagementPolicyPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `ManagementPolicyPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "require_all_affiliations_in_scope", nullable = false)
    private boolean requireAllAffiliationsInScope;

    /**
     * 构造器 `ManagementPolicyPO` 用于创建并初始化 `ManagementPolicyPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyPO` creates and initializes `ManagementPolicyPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementPolicyPO() {
    }

    /**
     * 构造器 `ManagementPolicyPO` 用于创建并初始化 `ManagementPolicyPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyPO` creates and initializes `ManagementPolicyPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
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
    public ManagementPolicyPO(
            Long id,
            Long tenantId,
            String policyCode,
            String name,
            Instant validFrom,
            Instant validTo,
            Integer maximumAssignmentDays,
            ManagementPolicyRiskLevelEnum maximumRiskLevel,
            ManagementPolicyAuthenticationStrengthEnum requiredAuthenticationStrength,
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
        this.status = ManagementPolicyStatusEnum.ACTIVE;
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
     * 方法 `getId` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getPolicyCode` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Policy Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPolicyCode` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Policy Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getName` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get ManagementPolicyStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get ManagementPolicyStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ManagementPolicyStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getValidFrom` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getValidTo` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getMaximumAssignmentDays` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Maximum Assignment Days` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumAssignmentDays` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Maximum Assignment Days` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getMaximumRiskLevel` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Maximum Risk Level` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumRiskLevel` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Maximum Risk Level` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMaximumRiskLevel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMaximumRiskLevel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ManagementPolicyRiskLevelEnum getMaximumRiskLevel() {
        return maximumRiskLevel;
    }

    /**
     * 方法 `getRequiredAuthenticationStrength` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `get Required Authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRequiredAuthenticationStrength` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `get Required Authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRequiredAuthenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRequiredAuthenticationStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ManagementPolicyAuthenticationStrengthEnum getRequiredAuthenticationStrength() {
        return requiredAuthenticationStrength;
    }

    /**
     * 方法 `isRequireReason` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `is Require Reason` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireReason` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `is Require Reason` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `isRequireTicket` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `is Require Ticket` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireTicket` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `is Require Ticket` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `isIncludeInheritedSubjectRoles` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `is Include Inherited Subject Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isIncludeInheritedSubjectRoles` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `is Include Inherited Subject Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `isRequireAllAffiliationsInScope` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `is Require All Affiliations In Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isRequireAllAffiliationsInScope` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `is Require All Affiliations In Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `update` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            ManagementPolicyRiskLevelEnum maximumRiskLevel,
            ManagementPolicyAuthenticationStrengthEnum requiredAuthenticationStrength,
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
     * 方法 `disable` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `disable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `disable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void disable(String actorId, Instant now) {
        if (status != ManagementPolicyStatusEnum.ACTIVE) {
            throw new IllegalStateException("only active management policy can be disabled");
        }
        status = ManagementPolicyStatusEnum.DISABLED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `validate` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `ManagementPolicyPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ManagementPolicyPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
