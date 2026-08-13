package top.egon.cola.platform.rbac3.admin.resource.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDataTypeEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionSensitivityEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionDefaultAccessEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.FieldDefinitionStatusEnum;

/**
 * 类型 `FieldDefinitionPO` 位于当前包内，是类型，用于承载 `Field Definition Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `FieldDefinitionPO` is a type in its package and carries the responsibility, state, or contract for `Field Definition Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `FieldDefinitionPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `FieldDefinitionPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "FieldDefinitionEntity")
@Table(name = "rbac3_field_definition")
public class FieldDefinitionPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `FieldDefinitionPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationId` 表示 `FieldDefinitionPO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 字段 `resourceId` 表示 `FieldDefinitionPO` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /**
     * 字段 `fieldCode` 表示 `FieldDefinitionPO` 中与 `field Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fieldCode` stores the `field Code`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fieldCode` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fieldCode`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "field_code", nullable = false, length = 128)
    private String fieldCode;

    /**
     * 字段 `jsonPath` 表示 `FieldDefinitionPO` 中与 `json Path` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jsonPath` stores the `json Path`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jsonPath` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jsonPath`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "json_path", nullable = false, length = 512)
    private String jsonPath;

    /**
     * 字段 `dataType` 表示 `FieldDefinitionPO` 中与 `data Type` 相关的状态、依赖、配置或结果（声明类型 `FieldDefinitionDataTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `dataType` stores the `data Type`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `FieldDefinitionDataTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `dataType` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `dataType`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 32)
    private FieldDefinitionDataTypeEnum dataType;

    /**
     * 字段 `sensitivity` 表示 `FieldDefinitionPO` 中与 `sensitivity` 相关的状态、依赖、配置或结果（声明类型 `FieldDefinitionSensitivityEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sensitivity` stores the `sensitivity`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `FieldDefinitionSensitivityEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sensitivity` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sensitivity`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FieldDefinitionSensitivityEnum sensitivity;

    /**
     * 字段 `defaultAccess` 表示 `FieldDefinitionPO` 中与 `default Access` 相关的状态、依赖、配置或结果（声明类型 `FieldDefinitionDefaultAccessEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `defaultAccess` stores the `default Access`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `FieldDefinitionDefaultAccessEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `defaultAccess` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `defaultAccess`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_access", nullable = false, length = 32)
    private FieldDefinitionDefaultAccessEnum defaultAccess;

    /**
     * 字段 `maskingStrategy` 表示 `FieldDefinitionPO` 中与 `masking Strategy` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maskingStrategy` stores the `masking Strategy`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maskingStrategy` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maskingStrategy`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "masking_strategy", length = 32)
    private String maskingStrategy;

    /**
     * 字段 `writable` 表示 `FieldDefinitionPO` 中与 `writable` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `writable` stores the `writable`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `writable` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `writable`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private boolean writable;

    /**
     * 字段 `exportable` 表示 `FieldDefinitionPO` 中与 `exportable` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `exportable` stores the `exportable`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `exportable` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `exportable`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private boolean exportable;

    /**
     * 字段 `status` 表示 `FieldDefinitionPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `FieldDefinitionStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `FieldDefinitionStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FieldDefinitionStatusEnum status;

    /**
     * 字段 `sourceManifestId` 表示 `FieldDefinitionPO` 中与 `source Manifest Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sourceManifestId` stores the `source Manifest Id`-related state, dependency, configuration, or result of `FieldDefinitionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sourceManifestId` 时应保持 `FieldDefinitionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sourceManifestId`, preserve `FieldDefinitionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "source_manifest_id")
    private Long sourceManifestId;

    /**
     * 构造器 `FieldDefinitionPO` 用于创建并初始化 `FieldDefinitionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `FieldDefinitionPO` creates and initializes `FieldDefinitionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `FieldDefinitionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `FieldDefinitionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected FieldDefinitionPO() {
    }

    /**
     * 构造器 `FieldDefinitionPO` 用于创建并初始化 `FieldDefinitionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `FieldDefinitionPO` creates and initializes `FieldDefinitionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `FieldDefinitionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `FieldDefinitionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldCode 输入参数 `fieldCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param jsonPath 输入参数 `jsonPath`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param dataType 输入参数 `dataType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sensitivity 输入参数 `sensitivity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param defaultAccess 输入参数 `defaultAccess`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maskingStrategy 输入参数 `maskingStrategy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param writable 输入参数 `writable`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param exportable 输入参数 `exportable`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sourceManifestId 输入参数 `sourceManifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public FieldDefinitionPO(
            Long id,
            Long tenantId,
            Long applicationId,
            Long resourceId,
            String fieldCode,
            String jsonPath,
            FieldDefinitionDataTypeEnum dataType,
            FieldDefinitionSensitivityEnum sensitivity,
            FieldDefinitionDefaultAccessEnum defaultAccess,
            String maskingStrategy,
            boolean writable,
            boolean exportable,
            Long sourceManifestId,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.fieldCode = required(fieldCode, "fieldCode");
        this.jsonPath = required(jsonPath, "jsonPath");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        this.sensitivity = Objects.requireNonNull(sensitivity, "sensitivity");
        this.defaultAccess = Objects.requireNonNull(defaultAccess, "defaultAccess");
        this.maskingStrategy = maskingStrategy;
        this.writable = writable;
        this.exportable = exportable;
        this.status = FieldDefinitionStatusEnum.ACTIVE;
        this.sourceManifestId = sourceManifestId;
        markCreated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `FieldDefinitionPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `FieldDefinitionPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `FieldDefinitionPO` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `FieldDefinitionPO`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `FieldDefinitionPO` 的职责处理输入，完成 `get FieldDefinitionStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `FieldDefinitionPO`'s responsibility, performs the `get FieldDefinitionStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public FieldDefinitionStatusEnum getStatus() {
        return status;
    }





    /**
     * 方法 `required` 按照 `FieldDefinitionPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `FieldDefinitionPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
