package top.egon.cola.platform.rbac3.admin.directory.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.directory.domain.enums.UserPositionSnapshotStatusEnum;

/**
 * 类型 `UserPositionSnapshotPO` 位于当前包内，是类型，用于承载 `User Position Snapshot Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `UserPositionSnapshotPO` is a type in its package and carries the responsibility, state, or contract for `User Position Snapshot Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `UserPositionSnapshotPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `UserPositionSnapshotPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "UserPositionSnapshotEntity")
@Table(name = "rbac3_user_position_snapshot")
public class UserPositionSnapshotPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `UserPositionSnapshotPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `snapshotId` 表示 `UserPositionSnapshotPO` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    /**
     * 字段 `userId` 表示 `UserPositionSnapshotPO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 字段 `positionId` 表示 `UserPositionSnapshotPO` 中与 `position Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `positionId` stores the `position Id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `positionId` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `positionId`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "position_id", nullable = false)
    private Long positionId;

    /**
     * 字段 `orgUnitId` 表示 `UserPositionSnapshotPO` 中与 `org Unit Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `orgUnitId` stores the `org Unit Id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `orgUnitId` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `orgUnitId`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    /**
     * 字段 `primary` 表示 `UserPositionSnapshotPO` 中与 `primary` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `primary` stores the `primary`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `primary` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `primary`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "primary_flag", nullable = false)
    private boolean primary;

    /**
     * 字段 `validFrom` 表示 `UserPositionSnapshotPO` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `UserPositionSnapshotPO` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 字段 `status` 表示 `UserPositionSnapshotPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `UserPositionSnapshotStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `UserPositionSnapshotStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserPositionSnapshotStatusEnum status;

    /**
     * 字段 `externalAssignmentId` 表示 `UserPositionSnapshotPO` 中与 `external Assignment Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `externalAssignmentId` stores the `external Assignment Id`-related state, dependency, configuration, or result of `UserPositionSnapshotPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `externalAssignmentId` 时应保持 `UserPositionSnapshotPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `externalAssignmentId`, preserve `UserPositionSnapshotPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "external_assignment_id", length = 256)
    private String externalAssignmentId;

    /**
     * 构造器 `UserPositionSnapshotPO` 用于创建并初始化 `UserPositionSnapshotPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserPositionSnapshotPO` creates and initializes `UserPositionSnapshotPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserPositionSnapshotPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserPositionSnapshotPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected UserPositionSnapshotPO() {
    }

    /**
     * 构造器 `UserPositionSnapshotPO` 用于创建并初始化 `UserPositionSnapshotPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserPositionSnapshotPO` creates and initializes `UserPositionSnapshotPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserPositionSnapshotPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserPositionSnapshotPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param primary 输入参数 `primary`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserPositionSnapshotPO(
            Long id,
            Long tenantId,
            Long snapshotId,
            Long userId,
            Long positionId,
            Long orgUnitId,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        this(id, tenantId, snapshotId, userId, positionId, orgUnitId, primary,
                null, validFrom, validTo, actorId, now);
    }

    /**
     * 构造器 `UserPositionSnapshotPO` 用于创建并初始化 `UserPositionSnapshotPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserPositionSnapshotPO` creates and initializes `UserPositionSnapshotPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserPositionSnapshotPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserPositionSnapshotPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param primary 输入参数 `primary`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param externalAssignmentId 输入参数 `externalAssignmentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserPositionSnapshotPO(
            Long id,
            Long tenantId,
            Long snapshotId,
            Long userId,
            Long positionId,
            Long orgUnitId,
            boolean primary,
            String externalAssignmentId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.positionId = Objects.requireNonNull(positionId, "positionId");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.primary = primary;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.status = UserPositionSnapshotStatusEnum.ACTIVE;
        this.externalAssignmentId = externalAssignmentId;
        markCreated(actorId, now);
    }

    /**
     * 方法 `inactivate` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `inactivate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `inactivate` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `inactivate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `inactivate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `inactivate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean inactivate(String actorId, Instant now) {
        if (status != UserPositionSnapshotStatusEnum.ACTIVE) {
            return false;
        }
        status = UserPositionSnapshotStatusEnum.INACTIVE;
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `getUserId` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getUserId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getUserId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 方法 `getPositionId` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get Position Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPositionId` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get Position Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPositionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPositionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getPositionId() {
        return positionId;
    }

    /**
     * 方法 `getOrgUnitId` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get Org Unit Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getOrgUnitId` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get Org Unit Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getOrgUnitId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getOrgUnitId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getOrgUnitId() {
        return orgUnitId;
    }

    /**
     * 方法 `isPrimary` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `is Primary` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isPrimary` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `is Primary` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isPrimary` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isPrimary`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * 方法 `getValidFrom` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getValidTo` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getExternalAssignmentId` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get External Assignment Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getExternalAssignmentId` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get External Assignment Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getExternalAssignmentId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getExternalAssignmentId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getExternalAssignmentId() {
        return externalAssignmentId;
    }

    /**
     * 方法 `getStatus` 按照 `UserPositionSnapshotPO` 的职责处理输入，完成 `get UserPositionSnapshotStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `UserPositionSnapshotPO`'s responsibility, performs the `get UserPositionSnapshotStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public UserPositionSnapshotStatusEnum getStatus() {
        return status;
    }

    }
