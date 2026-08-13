package top.egon.cola.platform.rbac3.admin.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `ResourceEntity` 位于当前包内，是类型，用于承载 `Resource Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ResourceEntity` is a type in its package and carries the responsibility, state, or contract for `Resource Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ResourceEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ResourceEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_resource")
public class ResourceEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ResourceEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `applicationId` 表示 `ResourceEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /**
     * 字段 `resourceType` 表示 `ResourceEntity` 中与 `resource Type` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceType` stores the `resource Type`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceType` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceType`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;
    /**
     * 字段 `resourceCode` 表示 `ResourceEntity` 中与 `resource Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceCode` stores the `resource Code`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceCode` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceCode`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_code", nullable = false, length = 128)
    private String resourceCode;
    /**
     * 字段 `resourceName` 表示 `ResourceEntity` 中与 `resource Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceName` stores the `resource Name`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceName` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceName`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_name", nullable = false, length = 200)
    private String resourceName;
    /**
     * 字段 `parentResourceId` 表示 `ResourceEntity` 中与 `parent Resource Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `parentResourceId` stores the `parent Resource Id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `parentResourceId` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `parentResourceId`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "parent_resource_id")
    private Long parentResourceId;
    /**
     * 字段 `requiredPermissionId` 表示 `ResourceEntity` 中与 `required Permission Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requiredPermissionId` stores the `required Permission Id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requiredPermissionId` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requiredPermissionId`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "required_permission_id")
    private Long requiredPermissionId;
    /**
     * 字段 `status` 表示 `ResourceEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `sourceManifestId` 表示 `ResourceEntity` 中与 `source Manifest Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sourceManifestId` stores the `source Manifest Id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sourceManifestId` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sourceManifestId`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "source_manifest_id")
    private Long sourceManifestId;
    /**
     * 字段 `sourceBuildId` 表示 `ResourceEntity` 中与 `source Build Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sourceBuildId` stores the `source Build Id`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sourceBuildId` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sourceBuildId`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "source_build_id", length = 256)
    private String sourceBuildId;
    /**
     * 字段 `mechanicalFacts` 表示 `ResourceEntity` 中与 `mechanical Facts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mechanicalFacts` stores the `mechanical Facts`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mechanicalFacts` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mechanicalFacts`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mechanical_facts", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> mechanicalFacts;
    /**
     * 字段 `displayMetadata` 表示 `ResourceEntity` 中与 `display Metadata` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `displayMetadata` stores the `display Metadata`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `displayMetadata` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `displayMetadata`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> displayMetadata;
    /**
     * 字段 `staleSince` 表示 `ResourceEntity` 中与 `stale Since` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `staleSince` stores the `stale Since`-related state, dependency, configuration, or result of `ResourceEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `staleSince` 时应保持 `ResourceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `staleSince`, preserve `ResourceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "stale_since")
    private Instant staleSince;

    /**
     * 构造器 `ResourceEntity` 用于创建并初始化 `ResourceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ResourceEntity` creates and initializes `ResourceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ResourceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ResourceEntity() {
    }

    /**
     * 构造器 `ResourceEntity` 用于创建并初始化 `ResourceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ResourceEntity` creates and initializes `ResourceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ResourceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ResourceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceCode 输入参数 `resourceCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceName 输入参数 `resourceName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param parentResourceId 输入参数 `parentResourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requiredPermissionId 输入参数 `requiredPermissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sourceManifestId 输入参数 `sourceManifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sourceBuildId 输入参数 `sourceBuildId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mechanicalFacts 输入参数 `mechanicalFacts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param displayMetadata 输入参数 `displayMetadata`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ResourceEntity(
            Long id, Long tenantId, Long applicationId, ResourceType resourceType,
            String resourceCode, String resourceName, Long parentResourceId,
            Long requiredPermissionId, Long sourceManifestId, String sourceBuildId,
            Map<String, Object> mechanicalFacts, Map<String, Object> displayMetadata,
            String actorId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.resourceCode = required(resourceCode, "resourceCode");
        this.resourceName = required(resourceName, "resourceName");
        this.parentResourceId = parentResourceId;
        this.requiredPermissionId = requiredPermissionId;
        this.status = Status.PENDING_VALIDATION;
        this.sourceManifestId = sourceManifestId;
        this.sourceBuildId = sourceBuildId;
        this.mechanicalFacts = Map.copyOf(mechanicalFacts);
        this.displayMetadata = Map.copyOf(displayMetadata);
        markCreated(actorId, now);
    }

    /**
     * 方法 `activate` 按照 `ResourceEntity` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `ResourceEntity`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activate(String actorId, Instant now) {
        status = Status.ACTIVE;
        staleSince = null;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `markStale` 按照 `ResourceEntity` 的职责处理输入，完成 `mark Stale` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markStale` processes its inputs according to `ResourceEntity`'s responsibility, performs the `mark Stale` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markStale` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markStale`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void markStale(String actorId, Instant now) {
        if (status == Status.ACTIVE) {
            status = Status.STALE;
            staleSince = now;
            markUpdated(actorId, now);
        }
    }

    /**
     * 方法 `archive` 按照 `ResourceEntity` 的职责处理输入，完成 `archive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archive` processes its inputs according to `ResourceEntity`'s responsibility, performs the `archive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void archive(String actorId, Instant now) {
        if (status != Status.STALE) {
            throw new IllegalStateException("only stale resource can be archived");
        }
        status = Status.ARCHIVED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `ResourceEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `ResourceEntity` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getResourceType` 按照 `ResourceEntity` 的职责处理输入，完成 `get Resource Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResourceType` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Resource Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResourceType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResourceType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * 方法 `getResourceCode` 按照 `ResourceEntity` 的职责处理输入，完成 `get Resource Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResourceCode` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Resource Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResourceCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResourceCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getResourceCode() {
        return resourceCode;
    }

    /**
     * 方法 `getResourceName` 按照 `ResourceEntity` 的职责处理输入，完成 `get Resource Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResourceName` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Resource Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResourceName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResourceName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * 方法 `getParentResourceId` 按照 `ResourceEntity` 的职责处理输入，完成 `get Parent Resource Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getParentResourceId` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Parent Resource Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getParentResourceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getParentResourceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getParentResourceId() {
        return parentResourceId;
    }

    /**
     * 方法 `getRequiredPermissionId` 按照 `ResourceEntity` 的职责处理输入，完成 `get Required Permission Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRequiredPermissionId` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Required Permission Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRequiredPermissionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRequiredPermissionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getRequiredPermissionId() {
        return requiredPermissionId;
    }

    /**
     * 方法 `getStatus` 按照 `ResourceEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSourceManifestId` 按照 `ResourceEntity` 的职责处理输入，完成 `get Source Manifest Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSourceManifestId` processes its inputs according to `ResourceEntity`'s responsibility, performs the `get Source Manifest Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSourceManifestId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSourceManifestId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getSourceManifestId() {
        return sourceManifestId;
    }

    /**
     * 类型 `ResourceType` 位于 `ResourceEntity` 内，是枚举，用于承载 `Resource Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResourceType` is an enum inside `ResourceEntity` and carries the responsibility, state, or contract for `Resource Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResourceType` 作为 `ResourceEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceType` as the responsibility boundary of `ResourceEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ResourceType {
        /**
         * 字段 `APP` 表示 `ResourceType` 中与 `APP` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `APP` stores the `APP`-related state, dependency, configuration, or result of `ResourceType` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `APP` 时应保持 `ResourceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `APP`, preserve `ResourceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        APP,
        /**
         * 字段 `MENU` 表示 `ResourceType` 中与 `MENU` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MENU` stores the `MENU`-related state, dependency, configuration, or result of `ResourceType` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MENU` 时应保持 `ResourceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MENU`, preserve `ResourceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        MENU,
        /**
         * 字段 `ROUTE` 表示 `ResourceType` 中与 `ROUTE` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROUTE` stores the `ROUTE`-related state, dependency, configuration, or result of `ResourceType` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROUTE` 时应保持 `ResourceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROUTE`, preserve `ResourceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROUTE,
        /**
         * 字段 `ACTION` 表示 `ResourceType` 中与 `ACTION` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTION` stores the `ACTION`-related state, dependency, configuration, or result of `ResourceType` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTION` 时应保持 `ResourceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTION`, preserve `ResourceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTION,
        /**
         * 字段 `API` 表示 `ResourceType` 中与 `API` 相关的状态、依赖、配置或结果（声明类型 `ResourceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `API` stores the `API`-related state, dependency, configuration, or result of `ResourceType` (declared type `ResourceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `API` 时应保持 `ResourceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `API`, preserve `ResourceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        API
    }

    /**
     * 类型 `Status` 位于 `ResourceEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `ResourceEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `ResourceEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `ResourceEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `PENDING_VALIDATION` 表示 `Status` 中与 `PENDING VALIDATION` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PENDING_VALIDATION` stores the `PENDING VALIDATION`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PENDING_VALIDATION` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PENDING_VALIDATION`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        PENDING_VALIDATION,
        /**
         * 字段 `ACTIVE` 表示 `Status` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `STALE` 表示 `Status` 中与 `STALE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `STALE` stores the `STALE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `STALE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `STALE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        STALE,
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
     * 方法 `required` 按照 `ResourceEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ResourceEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return value.trim();
    }
}
