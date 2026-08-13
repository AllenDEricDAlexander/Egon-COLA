package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 类型 `RoleEntity` 位于当前包内，是类型，用于承载 `Role Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleEntity` is a type in its package and carries the responsibility, state, or contract for `Role Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_role")
public class RoleEntity extends TenantScopedEntity {

    /**
     * 字段 `CODE` 表示 `RoleEntity` 中与 `CODE` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CODE` stores the `CODE`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CODE` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CODE`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");

    /**
     * 字段 `id` 表示 `RoleEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `applicationId` 表示 `RoleEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /**
     * 字段 `roleCode` 表示 `RoleEntity` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `RoleEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_code", nullable = false, length = 128, updatable = false)
    private String roleCode;
    /**
     * 字段 `roleName` 表示 `RoleEntity` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `RoleEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleName` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleName`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_name", nullable = false, length = 200)
    private String roleName;
    /**
     * 字段 `roleType` 表示 `RoleEntity` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `RoleEntity` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleType` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleType`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 32)
    private RoleType roleType;
    /**
     * 字段 `riskLevel` 表示 `RoleEntity` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `RiskLevel`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `RoleEntity` (declared type `RiskLevel`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;
    /**
     * 字段 `privileged` 表示 `RoleEntity` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `RoleEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `privileged` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `privileged`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private boolean privileged;
    /**
     * 字段 `status` 表示 `RoleEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `landingRouteId` 表示 `RoleEntity` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "landing_route_id")
    private Long landingRouteId;
    /**
     * 字段 `landingPriority` 表示 `RoleEntity` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `RoleEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "landing_priority", nullable = false)
    private int landingPriority;
    /**
     * 字段 `maximumAssignmentDays` 表示 `RoleEntity` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `RoleEntity` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `RoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `RoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;

    /**
     * 构造器 `RoleEntity` 用于创建并初始化 `RoleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleEntity` creates and initializes `RoleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RoleEntity() {
    }

    /**
     * 构造器 `RoleEntity` 用于创建并初始化 `RoleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleEntity` creates and initializes `RoleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleCode 输入参数 `roleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleName 输入参数 `roleName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleType 输入参数 `roleType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param riskLevel 输入参数 `riskLevel`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param privileged 输入参数 `privileged`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param landingRouteId 输入参数 `landingRouteId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param landingPriority 输入参数 `landingPriority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            String roleCode,
            String roleName,
            RoleType roleType,
            RiskLevel riskLevel,
            boolean privileged,
            Long landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            String actorId,
            Instant now) {
        if (!CODE.matcher(roleCode).matches()) {
            throw new IllegalArgumentException("roleCode is invalid");
        }
        if (landingPriority < 0 || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("role limits are invalid");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.roleCode = roleCode;
        this.roleName = required(roleName, "roleName");
        this.roleType = Objects.requireNonNull(roleType, "roleType");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        this.privileged = privileged;
        this.status = Status.ACTIVE;
        this.landingRouteId = landingRouteId;
        this.landingPriority = landingPriority;
        this.maximumAssignmentDays = maximumAssignmentDays;
        markCreated(actorId, now);
    }

    /**
     * 方法 `toRoleNode` 按照 `RoleEntity` 的职责处理输入，完成 `to Role Node` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toRoleNode` processes its inputs according to `RoleEntity`'s responsibility, performs the `to Role Node` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `toRoleNode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `toRoleNode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleNode toRoleNode() {
        return new RoleNode(
                id.toString(),
                applicationId.toString(),
                roleCode,
                status == Status.ACTIVE,
                RoleNode.RiskLevel.valueOf(riskLevel.name()),
                privileged,
                landingRouteId == null ? null : landingRouteId.toString(),
                landingPriority);
    }

    /**
     * 方法 `update` 按照 `RoleEntity` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `RoleEntity`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleName 输入参数 `roleName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param landingRouteId 输入参数 `landingRouteId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param landingPriority 输入参数 `landingPriority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAssignmentDays 输入参数 `maximumAssignmentDays`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            String roleName,
            Status status,
            Long landingRouteId,
            int landingPriority,
            Integer maximumAssignmentDays,
            String actorId,
            Instant now) {
        if (landingPriority < 0
                || maximumAssignmentDays != null && maximumAssignmentDays < 1) {
            throw new IllegalArgumentException("role limits are invalid");
        }
        this.roleName = required(roleName, "roleName");
        this.status = Objects.requireNonNull(status, "status");
        this.landingRouteId = landingRouteId;
        this.landingPriority = landingPriority;
        this.maximumAssignmentDays = maximumAssignmentDays;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `RoleEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `RoleEntity` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleCode` 按照 `RoleEntity` 的职责处理输入，完成 `get Role Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleCode` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Role Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getRoleCode() {
        return roleCode;
    }

    /**
     * 方法 `getRoleName` 按照 `RoleEntity` 的职责处理输入，完成 `get Role Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleName` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Role Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * 方法 `getRoleType` 按照 `RoleEntity` 的职责处理输入，完成 `get Role Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleType` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Role Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleType getRoleType() {
        return roleType;
    }

    /**
     * 方法 `getRiskLevel` 按照 `RoleEntity` 的职责处理输入，完成 `get Risk Level` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRiskLevel` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Risk Level` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRiskLevel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRiskLevel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    /**
     * 方法 `isPrivileged` 按照 `RoleEntity` 的职责处理输入，完成 `is Privileged` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isPrivileged` processes its inputs according to `RoleEntity`'s responsibility, performs the `is Privileged` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isPrivileged` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isPrivileged`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isPrivileged() {
        return privileged;
    }

    /**
     * 方法 `getStatus` 按照 `RoleEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getMaximumAssignmentDays` 按照 `RoleEntity` 的职责处理输入，完成 `get Maximum Assignment Days` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumAssignmentDays` processes its inputs according to `RoleEntity`'s responsibility, performs the `get Maximum Assignment Days` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `RoleType` 位于 `RoleEntity` 内，是枚举，用于承载 `Role Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleType` is an enum inside `RoleEntity` and carries the responsibility, state, or contract for `Role Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleType` 作为 `RoleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleType` as the responsibility boundary of `RoleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum RoleType {
        /**
         * 字段 `PUBLIC` 表示 `RoleType` 中与 `PUBLIC` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PUBLIC` stores the `PUBLIC`-related state, dependency, configuration, or result of `RoleType` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PUBLIC` 时应保持 `RoleType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PUBLIC`, preserve `RoleType`'s lifecycle, immutability, and thread-safety constraints.
         */
        PUBLIC,
        /**
         * 字段 `POSITION` 表示 `RoleType` 中与 `POSITION` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `POSITION` stores the `POSITION`-related state, dependency, configuration, or result of `RoleType` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `POSITION` 时应保持 `RoleType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `POSITION`, preserve `RoleType`'s lifecycle, immutability, and thread-safety constraints.
         */
        POSITION,
        /**
         * 字段 `MANAGEMENT` 表示 `RoleType` 中与 `MANAGEMENT` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MANAGEMENT` stores the `MANAGEMENT`-related state, dependency, configuration, or result of `RoleType` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MANAGEMENT` 时应保持 `RoleType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MANAGEMENT`, preserve `RoleType`'s lifecycle, immutability, and thread-safety constraints.
         */
        MANAGEMENT,
        /**
         * 字段 `TEMPORARY` 表示 `RoleType` 中与 `TEMPORARY` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `TEMPORARY` stores the `TEMPORARY`-related state, dependency, configuration, or result of `RoleType` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `TEMPORARY` 时应保持 `RoleType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `TEMPORARY`, preserve `RoleType`'s lifecycle, immutability, and thread-safety constraints.
         */
        TEMPORARY,
        /**
         * 字段 `EMERGENCY` 表示 `RoleType` 中与 `EMERGENCY` 相关的状态、依赖、配置或结果（声明类型 `RoleType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EMERGENCY` stores the `EMERGENCY`-related state, dependency, configuration, or result of `RoleType` (declared type `RoleType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EMERGENCY` 时应保持 `RoleType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EMERGENCY`, preserve `RoleType`'s lifecycle, immutability, and thread-safety constraints.
         */
        EMERGENCY
    }

    /**
     * 类型 `RiskLevel` 位于 `RoleEntity` 内，是枚举，用于承载 `Risk Level` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RiskLevel` is an enum inside `RoleEntity` and carries the responsibility, state, or contract for `Risk Level`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RiskLevel` 作为 `RoleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RiskLevel` as the responsibility boundary of `RoleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
     * 类型 `Status` 位于 `RoleEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `RoleEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `RoleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `RoleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
         * 字段 `ARCHIVED` 表示 `Status` 中与 `ARCHIVED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ARCHIVED` stores the `ARCHIVED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ARCHIVED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ARCHIVED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ARCHIVED
    }

    /**
     * 方法 `required` 按照 `RoleEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `RoleEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
