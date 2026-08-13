package top.egon.cola.platform.rbac3.admin.resource.domain;

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
 * 类型 `PermissionEntity` 位于当前包内，是类型，用于承载 `Permission Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PermissionEntity` is a type in its package and carries the responsibility, state, or contract for `Permission Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `PermissionEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `PermissionEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_permission")
public class PermissionEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `PermissionEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationId` 表示 `PermissionEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 字段 `permissionCode` 表示 `PermissionEntity` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "permission_code", nullable = false, length = 128)
    private String permissionCode;

    /**
     * 字段 `permissionName` 表示 `PermissionEntity` 中与 `permission Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `permissionName` stores the `permission Name`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `permissionName` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `permissionName`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "permission_name", nullable = false, length = 200)
    private String permissionName;

    /**
     * 字段 `riskLevel` 表示 `PermissionEntity` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    /**
     * 字段 `status` 表示 `PermissionEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 字段 `description` 表示 `PermissionEntity` 中与 `description` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `description` stores the `description`-related state, dependency, configuration, or result of `PermissionEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `description` 时应保持 `PermissionEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `description`, preserve `PermissionEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * 构造器 `PermissionEntity` 用于创建并初始化 `PermissionEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PermissionEntity` creates and initializes `PermissionEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PermissionEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PermissionEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected PermissionEntity() {
    }

    /**
     * 构造器 `PermissionEntity` 用于创建并初始化 `PermissionEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PermissionEntity` creates and initializes `PermissionEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PermissionEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PermissionEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionName 输入参数 `permissionName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param riskLevel 输入参数 `riskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param description 输入参数 `description`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PermissionEntity(Long id, Long tenantId, Long applicationId, String permissionCode,
                            String permissionName, RiskLevel riskLevel, String description,
                            String actorId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.permissionCode = required(permissionCode, "permissionCode");
        this.permissionName = required(permissionName, "permissionName");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        this.description = description;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `PermissionEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `PermissionEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `PermissionEntity` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `PermissionEntity`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getPermissionCode` 按照 `PermissionEntity` 的职责处理输入，完成 `get Permission Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPermissionCode` processes its inputs according to `PermissionEntity`'s responsibility, performs the `get Permission Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPermissionCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPermissionCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPermissionCode() {
        return permissionCode;
    }

    /**
     * 方法 `getStatus` 按照 `PermissionEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `PermissionEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `RiskLevel` 位于 `PermissionEntity` 内，是枚举，用于承载 `Risk Level` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RiskLevel` is an enum inside `PermissionEntity` and carries the responsibility, state, or contract for `Risk Level`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RiskLevel` 作为 `PermissionEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RiskLevel` as the responsibility boundary of `PermissionEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
     * 类型 `Status` 位于 `PermissionEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `PermissionEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `PermissionEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `PermissionEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
         * 字段 `DEPRECATED` 表示 `Status` 中与 `DEPRECATED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPRECATED` stores the `DEPRECATED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPRECATED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPRECATED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPRECATED,
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
     * 方法 `required` 按照 `PermissionEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `PermissionEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
