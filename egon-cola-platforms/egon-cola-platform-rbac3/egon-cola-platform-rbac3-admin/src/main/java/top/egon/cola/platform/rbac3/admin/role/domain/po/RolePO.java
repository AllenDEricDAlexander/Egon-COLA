package top.egon.cola.platform.rbac3.admin.role.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;
import top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleTypeEnum;
import top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleRiskLevelEnum;
import top.egon.cola.platform.rbac3.admin.role.domain.enums.RoleStatusEnum;

/**
 * 类型 `RolePO` 位于当前包内，是类型，用于承载 `Role Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RolePO` is a type in its package and carries the responsibility, state, or contract for `Role Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RolePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RolePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "RoleEntity")
@Table(name = "rbac3_role")
public class RolePO extends TenantScopedPO {

    /**
     * 字段 `CODE` 表示 `RolePO` 中与 `CODE` 相关的状态、依赖、配置或结果（声明类型 `Pattern`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CODE` stores the `CODE`-related state, dependency, configuration, or result of `RolePO` (declared type `Pattern`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CODE` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CODE`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");

    /**
     * 字段 `id` 表示 `RolePO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `applicationId` 表示 `RolePO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /**
     * 字段 `roleCode` 表示 `RolePO` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `RolePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_code", nullable = false, length = 128, updatable = false)
    private String roleCode;
    /**
     * 字段 `roleName` 表示 `RolePO` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `RolePO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleName` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleName`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_name", nullable = false, length = 200)
    private String roleName;
    /**
     * 字段 `roleType` 表示 `RolePO` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `RoleTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `RolePO` (declared type `RoleTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleType` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleType`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 32)
    private RoleTypeEnum roleType;
    /**
     * 字段 `riskLevel` 表示 `RolePO` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `RoleRiskLevelEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `RolePO` (declared type `RoleRiskLevelEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RoleRiskLevelEnum riskLevel;
    /**
     * 字段 `privileged` 表示 `RolePO` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `RolePO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `privileged` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `privileged`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private boolean privileged;
    /**
     * 字段 `status` 表示 `RolePO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `RoleStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `RolePO` (declared type `RoleStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoleStatusEnum status;
    /**
     * 字段 `landingRouteId` 表示 `RolePO` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `RolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "landing_route_id")
    private Long landingRouteId;
    /**
     * 字段 `landingPriority` 表示 `RolePO` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `RolePO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "landing_priority", nullable = false)
    private int landingPriority;
    /**
     * 字段 `maximumAssignmentDays` 表示 `RolePO` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `RolePO` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `RolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `RolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_assignment_days")
    private Integer maximumAssignmentDays;

    /**
     * 构造器 `RolePO` 用于创建并初始化 `RolePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RolePO` creates and initializes `RolePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RolePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RolePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RolePO() {
    }

    /**
     * 构造器 `RolePO` 用于创建并初始化 `RolePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RolePO` creates and initializes `RolePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RolePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RolePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
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
    public RolePO(
            Long id,
            Long tenantId,
            Long applicationId,
            String roleCode,
            String roleName,
            RoleTypeEnum roleType,
            RoleRiskLevelEnum riskLevel,
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
        this.status = RoleStatusEnum.ACTIVE;
        this.landingRouteId = landingRouteId;
        this.landingPriority = landingPriority;
        this.maximumAssignmentDays = maximumAssignmentDays;
        markCreated(actorId, now);
    }

    /**
     * 方法 `toRoleNode` 按照 `RolePO` 的职责处理输入，完成 `to Role Node` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `toRoleNode` processes its inputs according to `RolePO`'s responsibility, performs the `to Role Node` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
                status == RoleStatusEnum.ACTIVE,
                RoleNode.RiskLevel.valueOf(riskLevel.name()),
                privileged,
                landingRouteId == null ? null : landingRouteId.toString(),
                landingPriority);
    }

    /**
     * 方法 `update` 按照 `RolePO` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `RolePO`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
            RoleStatusEnum status,
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
     * 方法 `getId` 按照 `RolePO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `RolePO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `RolePO` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `RolePO`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleCode` 按照 `RolePO` 的职责处理输入，完成 `get Role Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleCode` processes its inputs according to `RolePO`'s responsibility, performs the `get Role Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleName` 按照 `RolePO` 的职责处理输入，完成 `get Role Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleName` processes its inputs according to `RolePO`'s responsibility, performs the `get Role Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleType` 按照 `RolePO` 的职责处理输入，完成 `get Role Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleType` processes its inputs according to `RolePO`'s responsibility, performs the `get Role Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleTypeEnum getRoleType() {
        return roleType;
    }

    /**
     * 方法 `getRiskLevel` 按照 `RolePO` 的职责处理输入，完成 `get Risk Level` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRiskLevel` processes its inputs according to `RolePO`'s responsibility, performs the `get Risk Level` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRiskLevel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRiskLevel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleRiskLevelEnum getRiskLevel() {
        return riskLevel;
    }

    /**
     * 方法 `isPrivileged` 按照 `RolePO` 的职责处理输入，完成 `is Privileged` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isPrivileged` processes its inputs according to `RolePO`'s responsibility, performs the `is Privileged` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `RolePO` 的职责处理输入，完成 `get RoleStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `RolePO`'s responsibility, performs the `get RoleStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getMaximumAssignmentDays` 按照 `RolePO` 的职责处理输入，完成 `get Maximum Assignment Days` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMaximumAssignmentDays` processes its inputs according to `RolePO`'s responsibility, performs the `get Maximum Assignment Days` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `RolePO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `RolePO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
