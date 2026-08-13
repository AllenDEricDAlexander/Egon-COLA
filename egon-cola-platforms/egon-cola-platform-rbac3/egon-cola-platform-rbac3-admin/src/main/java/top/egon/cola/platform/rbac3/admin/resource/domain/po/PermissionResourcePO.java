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
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.PermissionResourceStatusEnum;
import top.egon.cola.platform.rbac3.admin.resource.domain.enums.ResourceTypeEnum;

/**
 * 类型 `PermissionResourcePO` 位于当前包内，是类型，用于承载 `Permission Resource Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `PermissionResourcePO` is a type in its package and carries the responsibility, state, or contract for `Permission Resource Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `PermissionResourcePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `PermissionResourcePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "PermissionResourceEntity")
@Table(name = "rbac3_permission_resource")
public class PermissionResourcePO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `PermissionResourcePO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `applicationId` 表示 `PermissionResourcePO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /**
     * 字段 `permissionId` 表示 `PermissionResourcePO` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 字段 `resourceId` 表示 `PermissionResourcePO` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /**
     * 字段 `resourceType` 表示 `PermissionResourcePO` 中与 `resource Type` 相关的状态、依赖、配置或结果（声明类型 `ResourceTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceType` stores the `resource Type`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `ResourceTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceType` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceType`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceTypeEnum resourceType;

    /**
     * 字段 `definitionSetId` 表示 `PermissionResourcePO` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "definition_set_id", length = 64)
    private String definitionSetId;

    /**
     * 字段 `gatewayOperationId` 表示 `PermissionResourcePO` 中与 `gateway Operation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayOperationId` stores the `gateway Operation Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayOperationId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayOperationId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "gateway_operation_id", length = 64)
    private String gatewayOperationId;

    /**
     * 字段 `securityPolicyId` 表示 `PermissionResourcePO` 中与 `security Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `securityPolicyId` stores the `security Policy Id`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `securityPolicyId` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `securityPolicyId`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "security_policy_id", length = 128)
    private String securityPolicyId;

    /**
     * 字段 `mappingVersion` 表示 `PermissionResourcePO` 中与 `mapping Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mappingVersion` stores the `mapping Version`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mappingVersion` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mappingVersion`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "mapping_version", nullable = false)
    private long mappingVersion;

    /**
     * 字段 `status` 表示 `PermissionResourcePO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `PermissionResourceStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `PermissionResourcePO` (declared type `PermissionResourceStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `PermissionResourcePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `PermissionResourcePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PermissionResourceStatusEnum status;

    /**
     * 构造器 `PermissionResourcePO` 用于创建并初始化 `PermissionResourcePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PermissionResourcePO` creates and initializes `PermissionResourcePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PermissionResourcePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PermissionResourcePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected PermissionResourcePO() {
    }

    /**
     * 构造器 `PermissionResourcePO` 用于创建并初始化 `PermissionResourcePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `PermissionResourcePO` creates and initializes `PermissionResourcePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `PermissionResourcePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `PermissionResourcePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param definitionSetId 输入参数 `definitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayOperationId 输入参数 `gatewayOperationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param securityPolicyId 输入参数 `securityPolicyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mappingVersion 输入参数 `mappingVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public PermissionResourcePO(Long id, Long tenantId, Long applicationId, Long permissionId,
                                    Long resourceId, ResourceTypeEnum resourceType,
                                    String definitionSetId, String gatewayOperationId,
                                    String securityPolicyId, long mappingVersion,
                                    String actorId, Instant now) {
        boolean apiIdentity = resourceType == ResourceTypeEnum.API
                && present(definitionSetId) && present(gatewayOperationId);
        boolean nonApiIdentity = resourceType != ResourceTypeEnum.API
                && definitionSetId == null && gatewayOperationId == null;
        if (!apiIdentity && !nonApiIdentity) {
            throw new IllegalArgumentException("invalid API operation identity");
        }
        if (mappingVersion < 0) {
            throw new IllegalArgumentException("mappingVersion must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.definitionSetId = definitionSetId;
        this.gatewayOperationId = gatewayOperationId;
        this.securityPolicyId = securityPolicyId;
        this.mappingVersion = mappingVersion;
        this.status = PermissionResourceStatusEnum.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `present` 按照 `PermissionResourcePO` 的职责处理输入，完成 `present` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `present` processes its inputs according to `PermissionResourcePO`'s responsibility, performs the `present` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `present` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `present`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    }
