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
 * 类型 `FieldRuleEntity` 位于当前包内，是类型，用于承载 `Field Rule Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `FieldRuleEntity` is a type in its package and carries the responsibility, state, or contract for `Field Rule Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `FieldRuleEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `FieldRuleEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_field_rule")
public class FieldRuleEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `FieldRuleEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationId` 表示 `FieldRuleEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 字段 `roleId` 表示 `FieldRuleEntity` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 字段 `permissionId` 表示 `FieldRuleEntity` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 字段 `fieldDefinitionId` 表示 `FieldRuleEntity` 中与 `field Definition Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fieldDefinitionId` stores the `field Definition Id`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fieldDefinitionId` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fieldDefinitionId`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "field_definition_id", nullable = false)
    private Long fieldDefinitionId;

    /**
     * 字段 `accessLevel` 表示 `FieldRuleEntity` 中与 `access Level` 相关的状态、依赖、配置或结果（声明类型 `AccessLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `accessLevel` stores the `access Level`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `AccessLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `accessLevel` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `accessLevel`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 32)
    private AccessLevel accessLevel;

    /**
     * 字段 `validFrom` 表示 `FieldRuleEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `FieldRuleEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 字段 `status` 表示 `FieldRuleEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `FieldRuleEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `FieldRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `FieldRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 构造器 `FieldRuleEntity` 用于创建并初始化 `FieldRuleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `FieldRuleEntity` creates and initializes `FieldRuleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `FieldRuleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `FieldRuleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected FieldRuleEntity() {
    }

    /**
     * 构造器 `FieldRuleEntity` 用于创建并初始化 `FieldRuleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `FieldRuleEntity` creates and initializes `FieldRuleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `FieldRuleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `FieldRuleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldDefinitionId 输入参数 `fieldDefinitionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param accessLevel 输入参数 `accessLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public FieldRuleEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long roleId,
            Long permissionId,
            Long fieldDefinitionId,
            AccessLevel accessLevel,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.fieldDefinitionId = Objects.requireNonNull(
                fieldDefinitionId, "fieldDefinitionId");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `update` 按照 `FieldRuleEntity` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param accessLevel 输入参数 `accessLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            AccessLevel accessLevel,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleId` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleId` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * 方法 `getPermissionId` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Permission Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPermissionId` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Permission Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPermissionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPermissionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getPermissionId() {
        return permissionId;
    }

    /**
     * 方法 `getFieldDefinitionId` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Field Definition Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getFieldDefinitionId` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Field Definition Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getFieldDefinitionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getFieldDefinitionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getFieldDefinitionId() {
        return fieldDefinitionId;
    }

    /**
     * 方法 `getAccessLevel` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Access Level` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAccessLevel` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Access Level` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAccessLevel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAccessLevel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * 方法 `getStatus` 按照 `FieldRuleEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `AccessLevel` 位于 `FieldRuleEntity` 内，是枚举，用于承载 `Access Level` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AccessLevel` is an enum inside `FieldRuleEntity` and carries the responsibility, state, or contract for `Access Level`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AccessLevel` 作为 `FieldRuleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AccessLevel` as the responsibility boundary of `FieldRuleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum AccessLevel {
        /**
         * 字段 `NONE` 表示 `AccessLevel` 中与 `NONE` 相关的状态、依赖、配置或结果（声明类型 `AccessLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `NONE` stores the `NONE`-related state, dependency, configuration, or result of `AccessLevel` (declared type `AccessLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `NONE` 时应保持 `AccessLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `NONE`, preserve `AccessLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        NONE,
        /**
         * 字段 `MASKED_READ` 表示 `AccessLevel` 中与 `MASKED READ` 相关的状态、依赖、配置或结果（声明类型 `AccessLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MASKED_READ` stores the `MASKED READ`-related state, dependency, configuration, or result of `AccessLevel` (declared type `AccessLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MASKED_READ` 时应保持 `AccessLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MASKED_READ`, preserve `AccessLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        MASKED_READ,
        /**
         * 字段 `READ` 表示 `AccessLevel` 中与 `READ` 相关的状态、依赖、配置或结果（声明类型 `AccessLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `READ` stores the `READ`-related state, dependency, configuration, or result of `AccessLevel` (declared type `AccessLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `READ` 时应保持 `AccessLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `READ`, preserve `AccessLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        READ,
        /**
         * 字段 `WRITE` 表示 `AccessLevel` 中与 `WRITE` 相关的状态、依赖、配置或结果（声明类型 `AccessLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `WRITE` stores the `WRITE`-related state, dependency, configuration, or result of `AccessLevel` (declared type `AccessLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `WRITE` 时应保持 `AccessLevel` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `WRITE`, preserve `AccessLevel`'s lifecycle, immutability, and thread-safety constraints.
         */
        WRITE
    }

    /**
     * 类型 `Status` 位于 `FieldRuleEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `FieldRuleEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `FieldRuleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `FieldRuleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
     * 方法 `validateWindow` 按照 `FieldRuleEntity` 的职责处理输入，完成 `validate Window` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateWindow` processes its inputs according to `FieldRuleEntity`'s responsibility, performs the `validate Window` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateWindow` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateWindow`, then continue the business flow using its result, exception, or side effect.
     *
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void validateWindow(Instant validFrom, Instant validTo) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }
}
