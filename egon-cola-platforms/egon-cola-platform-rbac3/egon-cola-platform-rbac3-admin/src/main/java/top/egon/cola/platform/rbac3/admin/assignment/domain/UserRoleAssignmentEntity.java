package top.egon.cola.platform.rbac3.admin.assignment.domain;

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
 * 类型 `UserRoleAssignmentEntity` 位于当前包内，是类型，用于承载 `User Role Assignment Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `UserRoleAssignmentEntity` is a type in its package and carries the responsibility, state, or contract for `User Role Assignment Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `UserRoleAssignmentEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `UserRoleAssignmentEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_user_role_assignment")
public class UserRoleAssignmentEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `UserRoleAssignmentEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `userId` 表示 `UserRoleAssignmentEntity` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    /**
     * 字段 `roleId` 表示 `UserRoleAssignmentEntity` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    /**
     * 字段 `assignmentType` 表示 `UserRoleAssignmentEntity` 中与 `assignment Type` 相关的状态、依赖、配置或结果（声明类型 `AssignmentType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `assignmentType` stores the `assignment Type`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `AssignmentType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `assignmentType` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `assignmentType`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 32)
    private AssignmentType assignmentType;
    /**
     * 字段 `status` 表示 `UserRoleAssignmentEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `validFrom` 表示 `UserRoleAssignmentEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `UserRoleAssignmentEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;
    /**
     * 字段 `sourceType` 表示 `UserRoleAssignmentEntity` 中与 `source Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sourceType` stores the `source Type`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sourceType` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sourceType`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;
    /**
     * 字段 `sourceId` 表示 `UserRoleAssignmentEntity` 中与 `source Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sourceId` stores the `source Id`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sourceId` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sourceId`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;
    /**
     * 字段 `reason` 表示 `UserRoleAssignmentEntity` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `reason` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `reason`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(length = 500)
    private String reason;
    /**
     * 字段 `ticketNo` 表示 `UserRoleAssignmentEntity` 中与 `ticket No` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ticketNo` stores the `ticket No`-related state, dependency, configuration, or result of `UserRoleAssignmentEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ticketNo` 时应保持 `UserRoleAssignmentEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ticketNo`, preserve `UserRoleAssignmentEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "ticket_no", length = 128)
    private String ticketNo;

    /**
     * 构造器 `UserRoleAssignmentEntity` 用于创建并初始化 `UserRoleAssignmentEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserRoleAssignmentEntity` creates and initializes `UserRoleAssignmentEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserRoleAssignmentEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserRoleAssignmentEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected UserRoleAssignmentEntity() {
    }

    /**
     * 构造器 `UserRoleAssignmentEntity` 用于创建并初始化 `UserRoleAssignmentEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserRoleAssignmentEntity` creates and initializes `UserRoleAssignmentEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserRoleAssignmentEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserRoleAssignmentEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param assignmentType 输入参数 `assignmentType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sourceType 输入参数 `sourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sourceId 输入参数 `sourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ticketNo 输入参数 `ticketNo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserRoleAssignmentEntity(
            Long id,
            Long tenantId,
            Long userId,
            Long roleId,
            AssignmentType assignmentType,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            String reason,
            String ticketNo,
            String actorId,
            Instant now
    ) {
        if (validTo != null && !validTo.isAfter(validFrom)
                || (assignmentType == AssignmentType.TEMPORARY
                || assignmentType == AssignmentType.EMERGENCY) && validTo == null) {
            throw new IllegalArgumentException("invalid assignment window");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.assignmentType = Objects.requireNonNull(assignmentType, "assignmentType");
        this.status = validFrom.isAfter(now) ? Status.PENDING : Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.sourceType = required(sourceType, "sourceType");
        this.sourceId = required(sourceId, "sourceId");
        this.reason = optional(reason);
        this.ticketNo = optional(ticketNo);
        markCreated(actorId, now);
    }

    /**
     * 方法 `revoke` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void revoke(String actorId, Instant now) {
        if (status != Status.ACTIVE && status != Status.SUSPENDED) {
            throw new IllegalStateException("assignment is not revocable");
        }
        status = Status.REVOKED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `suspend` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `suspend` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `suspend` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `suspend` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `suspend` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `suspend`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void suspend(String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("assignment is not active");
        }
        status = Status.SUSPENDED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `resume` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `resume` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resume` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `resume` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resume` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resume`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void resume(String actorId, Instant now) {
        if (status != Status.SUSPENDED
                || validTo != null && !validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not resumable");
        }
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `activate` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activate(String actorId, Instant now) {
        if (status != Status.PENDING || validFrom.isAfter(now)
                || validTo != null && !validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not activatable");
        }
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `expire` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `expire` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `expire` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `expire` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `expire` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `expire`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void expire(String actorId, Instant now) {
        if (status != Status.ACTIVE && status != Status.SUSPENDED
                || validTo == null || validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not expirable");
        }
        status = Status.EXPIRED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getUserId` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getRoleId` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleId` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getAssignmentType` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Assignment Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAssignmentType` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Assignment Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAssignmentType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAssignmentType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    /**
     * 方法 `getSourceType` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Source Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSourceType` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Source Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSourceType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSourceType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 方法 `getSourceId` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Source Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSourceId` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Source Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSourceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSourceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * 方法 `getValidFrom` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Valid From` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidFrom` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Valid From` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getValidTo` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `get Valid To` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getValidTo` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `get Valid To` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 方法 `optional` 按照 `UserRoleAssignmentEntity` 的职责处理输入，完成 `optional` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `optional` processes its inputs according to `UserRoleAssignmentEntity`'s responsibility, performs the `optional` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `optional` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `optional`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String optional(String value) {
        return value == null ? null : required(value, "optional value");
    }

    /**
     * 类型 `AssignmentType` 位于 `UserRoleAssignmentEntity` 内，是枚举，用于承载 `Assignment Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignmentType` is an enum inside `UserRoleAssignmentEntity` and carries the responsibility, state, or contract for `Assignment Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignmentType` 作为 `UserRoleAssignmentEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignmentType` as the responsibility boundary of `UserRoleAssignmentEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum AssignmentType {
        /**
         * 字段 `AUTO` 表示 `AssignmentType` 中与 `AUTO` 相关的状态、依赖、配置或结果（声明类型 `AssignmentType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `AUTO` stores the `AUTO`-related state, dependency, configuration, or result of `AssignmentType` (declared type `AssignmentType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `AUTO` 时应保持 `AssignmentType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `AUTO`, preserve `AssignmentType`'s lifecycle, immutability, and thread-safety constraints.
         */
        AUTO,
        /**
         * 字段 `DIRECT` 表示 `AssignmentType` 中与 `DIRECT` 相关的状态、依赖、配置或结果（声明类型 `AssignmentType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DIRECT` stores the `DIRECT`-related state, dependency, configuration, or result of `AssignmentType` (declared type `AssignmentType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DIRECT` 时应保持 `AssignmentType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DIRECT`, preserve `AssignmentType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DIRECT,
        /**
         * 字段 `TEMPORARY` 表示 `AssignmentType` 中与 `TEMPORARY` 相关的状态、依赖、配置或结果（声明类型 `AssignmentType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `TEMPORARY` stores the `TEMPORARY`-related state, dependency, configuration, or result of `AssignmentType` (declared type `AssignmentType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `TEMPORARY` 时应保持 `AssignmentType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `TEMPORARY`, preserve `AssignmentType`'s lifecycle, immutability, and thread-safety constraints.
         */
        TEMPORARY,
        /**
         * 字段 `EMERGENCY` 表示 `AssignmentType` 中与 `EMERGENCY` 相关的状态、依赖、配置或结果（声明类型 `AssignmentType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EMERGENCY` stores the `EMERGENCY`-related state, dependency, configuration, or result of `AssignmentType` (declared type `AssignmentType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EMERGENCY` 时应保持 `AssignmentType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EMERGENCY`, preserve `AssignmentType`'s lifecycle, immutability, and thread-safety constraints.
         */
        EMERGENCY
    }

    /**
     * 类型 `Status` 位于 `UserRoleAssignmentEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `UserRoleAssignmentEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `UserRoleAssignmentEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `UserRoleAssignmentEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `PENDING` 表示 `Status` 中与 `PENDING` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PENDING` stores the `PENDING`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PENDING` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PENDING`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        PENDING,
        /**
         * 字段 `ACTIVE` 表示 `Status` 中与 `ACTIVE` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTIVE` stores the `ACTIVE`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTIVE` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTIVE`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTIVE,
        /**
         * 字段 `SUSPENDED` 表示 `Status` 中与 `SUSPENDED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SUSPENDED` stores the `SUSPENDED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SUSPENDED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SUSPENDED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        SUSPENDED,
        /**
         * 字段 `EXPIRED` 表示 `Status` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED,
        /**
         * 字段 `REVOKED` 表示 `Status` 中与 `REVOKED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKED` stores the `REVOKED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKED
    }
}
