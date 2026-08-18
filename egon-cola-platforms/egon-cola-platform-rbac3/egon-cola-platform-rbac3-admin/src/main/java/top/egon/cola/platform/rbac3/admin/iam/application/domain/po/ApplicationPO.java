package top.egon.cola.platform.rbac3.admin.iam.application.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.GlobalAuditedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.ApplicationStatusEnum;

/**
 * 类型 `ApplicationPO` 位于当前包内，是类型，用于承载 `Application Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ApplicationPO` is a type in its package and carries the responsibility, state, or contract for `Application Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ApplicationPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ApplicationPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ApplicationEntity")
@Table(name = "rbac3_application")
public class ApplicationPO extends GlobalAuditedPO {

    /** Compatibility-only value for old service signatures; it is never persisted. */
    private transient Long legacyTenantId;

    /**
     * 字段 `id` 表示 `ApplicationPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    @Column(name = "ddc_application_id", nullable = false, length = 64)
    private String ddcApplicationId;

    @Column(name = "ddc_business_id", nullable = false, length = 64)
    private String ddcBusinessId;

    /**
     * 字段 `applicationCode` 表示 `ApplicationPO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    /**
     * 字段 `applicationName` 表示 `ApplicationPO` 中与 `application Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationName` stores the `application Name`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationName` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationName`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_name", nullable = false, length = 200)
    private String applicationName;

    /**
     * 字段 `displayPriority` 表示 `ApplicationPO` 中与 `display Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `displayPriority` stores the `display Priority`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `displayPriority` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `displayPriority`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "display_priority", nullable = false)
    private int displayPriority;

    /**
     * 字段 `status` 表示 `ApplicationPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `ApplicationStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `ApplicationStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatusEnum status;

    /**
     * 字段 `currentManifestId` 表示 `ApplicationPO` 中与 `current Manifest Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `currentManifestId` stores the `current Manifest Id`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `currentManifestId` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `currentManifestId`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */

    /**
     * 字段 `currentManifestVersion` 表示 `ApplicationPO` 中与 `current Manifest Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `currentManifestVersion` stores the `current Manifest Version`-related state, dependency, configuration, or result of `ApplicationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `currentManifestVersion` 时应保持 `ApplicationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `currentManifestVersion`, preserve `ApplicationPO`'s lifecycle, immutability, and thread-safety constraints.
     */

    /**
     * 构造器 `ApplicationPO` 用于创建并初始化 `ApplicationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ApplicationPO` creates and initializes `ApplicationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ApplicationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ApplicationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ApplicationPO() {
    }

    /**
     * 构造器 `ApplicationPO` 用于创建并初始化 `ApplicationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ApplicationPO` creates and initializes `ApplicationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ApplicationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ApplicationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationName 输入参数 `applicationName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param displayPriority 输入参数 `displayPriority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ApplicationPO(
            Long id,
            Long tenantId,
            String ddcApplicationId,
            String ddcBusinessId,
            String applicationCode,
            String applicationName,
            int displayPriority,
            String actorId,
            Instant now) {
        if (displayPriority < 0) {
            throw new IllegalArgumentException("displayPriority must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.legacyTenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.ddcApplicationId = required(ddcApplicationId, "ddcApplicationId");
        this.ddcBusinessId = required(ddcBusinessId, "ddcBusinessId");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.applicationName = required(applicationName, "applicationName");
        this.displayPriority = displayPriority;
        this.status = ApplicationStatusEnum.ACTIVE;
        markCreated(actorId, now);
    }

    public ApplicationPO(
            Long id,
            Long tenantId,
            String applicationCode,
            String applicationName,
            int displayPriority,
            String actorId,
            Instant now) {
        if (displayPriority < 0) {
            throw new IllegalArgumentException("displayPriority must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.legacyTenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.applicationName = required(applicationName, "applicationName");
        this.displayPriority = displayPriority;
        this.status = ApplicationStatusEnum.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `activateManifest` 按照 `ApplicationPO` 的职责处理输入，完成 `activate Manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activateManifest` processes its inputs according to `ApplicationPO`'s responsibility, performs the `activate Manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activateManifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activateManifest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param manifestVersion 输入参数 `manifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */


    /** Changes the local authorization-scope status with optimistic concurrency. */
    public boolean changeStatus(
            ApplicationStatusEnum nextStatus,
            long expectedVersion,
            String actorId,
            Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        if (expectedVersion < 0L) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        if (getVersion() != expectedVersion) {
            throw new IllegalStateException("application version conflict");
        }
        if (status == nextStatus) {
            return false;
        }
        status = nextStatus;
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `getId` 按照 `ApplicationPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ApplicationPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getId() {
        return id;
    }

    /** Compatibility accessor for code being migrated to TenantApplicationPO. */
    @Transient
    public Long getTenantId() {
        return legacyTenantId;
    }

    public String getDdcApplicationId() {
        return ddcApplicationId;
    }

    public String getDdcBusinessId() {
        return ddcBusinessId;
    }

    /**
     * 方法 `getApplicationCode` 按照 `ApplicationPO` 的职责处理输入，完成 `get Application Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationCode` processes its inputs according to `ApplicationPO`'s responsibility, performs the `get Application Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * 方法 `getApplicationName` 按照 `ApplicationPO` 的职责处理输入，完成 `get Application Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationName` processes its inputs according to `ApplicationPO`'s responsibility, performs the `get Application Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getApplicationName() {
        return applicationName;
    }

    public int getDisplayPriority() {
        return displayPriority;
    }

    /**
     * 方法 `getStatus` 按照 `ApplicationPO` 的职责处理输入，完成 `get ApplicationStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ApplicationPO`'s responsibility, performs the `get ApplicationStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ApplicationStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getCurrentManifestVersion` 按照 `ApplicationPO` 的职责处理输入，完成 `get Current Manifest Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCurrentManifestVersion` processes its inputs according to `ApplicationPO`'s responsibility, performs the `get Current Manifest Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCurrentManifestVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCurrentManifestVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */


    /**
     * 方法 `required` 按照 `ApplicationPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ApplicationPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
