package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `SodSetEntity` 位于当前包内，是类型，用于承载 `Sod Set Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SodSetEntity` is a type in its package and carries the responsibility, state, or contract for `Sod Set Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SodSetEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SodSetEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_sod_set")
public class SodSetEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `SodSetEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `applicationId` 表示 `SodSetEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id")
    private Long applicationId;
    /**
     * 字段 `setCode` 表示 `SodSetEntity` 中与 `set Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `setCode` stores the `set Code`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `setCode` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `setCode`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "set_code", nullable = false, length = 128)
    private String setCode;
    /**
     * 字段 `constraintType` 表示 `SodSetEntity` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_type", nullable = false, length = 32)
    private ConstraintType constraintType;
    /**
     * 字段 `maximumActiveRoles` 表示 `SodSetEntity` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_active_roles", nullable = false)
    private int maximumActiveRoles;
    /**
     * 字段 `status` 表示 `SodSetEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `validFrom` 表示 `SodSetEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `SodSetEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SodSetEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SodSetEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SodSetEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `SodSetEntity` 用于创建并初始化 `SodSetEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SodSetEntity` creates and initializes `SodSetEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SodSetEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SodSetEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected SodSetEntity() {
    }

    /**
     * 构造器 `SodSetEntity` 用于创建并初始化 `SodSetEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SodSetEntity` creates and initializes `SodSetEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SodSetEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SodSetEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param setCode 输入参数 `setCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SodSetEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            String setCode,
            ConstraintType constraintType,
            int maximumActiveRoles,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (constraintType == ConstraintType.DSD && applicationId == null) {
            throw new IllegalArgumentException("DSD requires an application");
        }
        if (maximumActiveRoles < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid SOD set limits");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = applicationId;
        this.setCode = required(setCode, "setCode");
        this.constraintType = Objects.requireNonNull(constraintType, "constraintType");
        this.maximumActiveRoles = maximumActiveRoles;
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    /**
     * 方法 `update` 按照 `SodSetEntity` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `SodSetEntity`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            Long applicationId,
            ConstraintType constraintType,
            int maximumActiveRoles,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (constraintType == ConstraintType.DSD && applicationId == null) {
            throw new IllegalArgumentException("DSD requires an application");
        }
        if (maximumActiveRoles < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid SOD set limits");
        }
        this.applicationId = applicationId;
        this.constraintType = constraintType;
        this.maximumActiveRoles = maximumActiveRoles;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `SodSetEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `SodSetEntity` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * 方法 `getSetCode` 按照 `SodSetEntity` 的职责处理输入，完成 `get Set Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSetCode` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Set Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSetCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSetCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getSetCode() {
        return setCode;
    }

    /**
     * 方法 `getConstraintType` 按照 `SodSetEntity` 的职责处理输入，完成 `get Constraint Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getConstraintType` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Constraint Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getConstraintType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getConstraintType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ConstraintType getConstraintType() {
        return constraintType;
    }

    /**
     * 方法 `getMaximumActiveRoles` 按照 `SodSetEntity` 的职责处理输入，完成 `get Maximum Active Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumActiveRoles` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Maximum Active Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMaximumActiveRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMaximumActiveRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getMaximumActiveRoles() {
        return maximumActiveRoles;
    }

    /**
     * 方法 `getStatus` 按照 `SodSetEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `SodSetEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `ConstraintType` 位于 `SodSetEntity` 内，是枚举，用于承载 `Constraint Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConstraintType` is an enum inside `SodSetEntity` and carries the responsibility, state, or contract for `Constraint Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConstraintType` 作为 `SodSetEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConstraintType` as the responsibility boundary of `SodSetEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ConstraintType {
        /**
         * 字段 `SSD` 表示 `ConstraintType` 中与 `SSD` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SSD` stores the `SSD`-related state, dependency, configuration, or result of `ConstraintType` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SSD` 时应保持 `ConstraintType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SSD`, preserve `ConstraintType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SSD,
        /**
         * 字段 `DSD` 表示 `ConstraintType` 中与 `DSD` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DSD` stores the `DSD`-related state, dependency, configuration, or result of `ConstraintType` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DSD` 时应保持 `ConstraintType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DSD`, preserve `ConstraintType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DSD
    }

    /**
     * 类型 `Status` 位于 `SodSetEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `SodSetEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `SodSetEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `SodSetEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
        EXPIRED
    }

    /**
     * 方法 `required` 按照 `SodSetEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `SodSetEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
