package top.egon.cola.platform.rbac3.admin.runtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;

/**
 * 类型 `AuthorizationMutationEntity` 位于当前包内，是类型，用于承载 `Authorization Mutation Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationMutationEntity` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuthorizationMutationEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthorizationMutationEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_authorization_mutation")
public class AuthorizationMutationEntity extends TenantScopedPO {

    /**
     * 字段 `mutationId` 表示 `AuthorizationMutationEntity` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "mutation_id")
    private Long mutationId;
    /**
     * 字段 `userId` 表示 `AuthorizationMutationEntity` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id")
    private Long userId;
    /**
     * 字段 `sessionId` 表示 `AuthorizationMutationEntity` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_id")
    private Long sessionId;
    /**
     * 字段 `scopeType` 表示 `AuthorizationMutationEntity` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    /**
     * 字段 `commandId` 表示 `AuthorizationMutationEntity` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `commandId` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `commandId`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;
    /**
     * 字段 `status` 表示 `AuthorizationMutationEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `oldSessionVersion` 表示 `AuthorizationMutationEntity` 中与 `old Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldSessionVersion` stores the `old Session Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldSessionVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldSessionVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_session_version")
    private Long oldSessionVersion;
    /**
     * 字段 `newSessionVersion` 表示 `AuthorizationMutationEntity` 中与 `new Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newSessionVersion` stores the `new Session Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newSessionVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newSessionVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_session_version")
    private Long newSessionVersion;
    /**
     * 字段 `oldAuthVersion` 表示 `AuthorizationMutationEntity` 中与 `old Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldAuthVersion` stores the `old Auth Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldAuthVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldAuthVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_auth_version")
    private Long oldAuthVersion;
    /**
     * 字段 `newAuthVersion` 表示 `AuthorizationMutationEntity` 中与 `new Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newAuthVersion` stores the `new Auth Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newAuthVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newAuthVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_auth_version")
    private Long newAuthVersion;
    /**
     * 字段 `oldPolicyVersion` 表示 `AuthorizationMutationEntity` 中与 `old Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldPolicyVersion` stores the `old Policy Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldPolicyVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldPolicyVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_policy_version")
    private Long oldPolicyVersion;
    /**
     * 字段 `newPolicyVersion` 表示 `AuthorizationMutationEntity` 中与 `new Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newPolicyVersion` stores the `new Policy Version`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newPolicyVersion` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newPolicyVersion`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_policy_version")
    private Long newPolicyVersion;
    /**
     * 字段 `fenceCreatedAt` 表示 `AuthorizationMutationEntity` 中与 `fence Created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fenceCreatedAt` stores the `fence Created At`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fenceCreatedAt` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fenceCreatedAt`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "fence_created_at")
    private Instant fenceCreatedAt;
    /**
     * 字段 `committedAt` 表示 `AuthorizationMutationEntity` 中与 `committed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `committedAt` stores the `committed At`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `committedAt` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `committedAt`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "committed_at")
    private Instant committedAt;
    /**
     * 字段 `projectedAt` 表示 `AuthorizationMutationEntity` 中与 `projected At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `projectedAt` stores the `projected At`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `projectedAt` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `projectedAt`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "projected_at")
    private Instant projectedAt;
    /**
     * 字段 `completedAt` 表示 `AuthorizationMutationEntity` 中与 `completed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `completedAt` stores the `completed At`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `completedAt` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `completedAt`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    /**
     * 字段 `lastErrorCode` 表示 `AuthorizationMutationEntity` 中与 `last Error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastErrorCode` stores the `last Error Code`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastErrorCode` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastErrorCode`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "last_error_code", length = 128)
    private String lastErrorCode;
    /**
     * 字段 `attempt` 表示 `AuthorizationMutationEntity` 中与 `attempt` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `attempt` stores the `attempt`-related state, dependency, configuration, or result of `AuthorizationMutationEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `attempt` 时应保持 `AuthorizationMutationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `attempt`, preserve `AuthorizationMutationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private int attempt;

    /**
     * 构造器 `AuthorizationMutationEntity` 用于创建并初始化 `AuthorizationMutationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationEntity` creates and initializes `AuthorizationMutationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected AuthorizationMutationEntity() {
    }

    /**
     * 构造器 `AuthorizationMutationEntity` 用于创建并初始化 `AuthorizationMutationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationEntity` creates and initializes `AuthorizationMutationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param commandId 输入参数 `commandId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param oldSessionVersion 输入参数 `oldSessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param newSessionVersion 输入参数 `newSessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param oldAuthVersion 输入参数 `oldAuthVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param newAuthVersion 输入参数 `newAuthVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param oldPolicyVersion 输入参数 `oldPolicyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param newPolicyVersion 输入参数 `newPolicyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthorizationMutationEntity(
            Long mutationId,
            Long tenantId,
            Long userId,
            Long sessionId,
            ScopeType scopeType,
            String commandId,
            Long oldSessionVersion,
            Long newSessionVersion,
            Long oldAuthVersion,
            Long newAuthVersion,
            Long oldPolicyVersion,
            Long newPolicyVersion,
            String actorId,
            Instant now
    ) {
        this.mutationId = mutationId;
        setTenantId(tenantId);
        this.userId = userId;
        this.sessionId = sessionId;
        this.scopeType = scopeType;
        this.commandId = commandId;
        this.status = Status.PREPARING;
        this.oldSessionVersion = oldSessionVersion;
        this.newSessionVersion = newSessionVersion;
        this.oldAuthVersion = oldAuthVersion;
        this.newAuthVersion = newAuthVersion;
        this.oldPolicyVersion = oldPolicyVersion;
        this.newPolicyVersion = newPolicyVersion;
        markCreated(actorId, now);
    }

    /**
     * 方法 `committed` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `committed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `committed` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `committed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `committed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `committed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void committed(Instant now, String actorId) {
        status = Status.COMMITTED;
        committedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `fenced` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fenced` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fenced`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void fenced(Instant now, String actorId) {
        fenceCreatedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `projected` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `projected` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `projected` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `projected` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `projected` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `projected`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void projected(Instant now, String actorId) {
        status = Status.PROJECTED;
        projectedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `completed` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `completed` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `completed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `completed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void completed(Instant now, String actorId) {
        status = Status.COMPLETED;
        completedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `recoveryRequired` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `recovery Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recoveryRequired` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `recovery Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recoveryRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recoveryRequired`, then continue the business flow using its result, exception, or side effect.
     *
     * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void recoveryRequired(String errorCode, Instant now, String actorId) {
        status = Status.RECOVERY_REQUIRED;
        lastErrorCode = errorCode;
        attempt = Math.incrementExact(attempt);
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getMutationId` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Mutation Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMutationId` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Mutation Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getMutationId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getMutationId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getMutationId() {
        return mutationId;
    }

    /**
     * 方法 `getUserId` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSessionId` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionId` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSessionId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSessionId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getSessionId() {
        return sessionId;
    }

    /**
     * 方法 `getScopeType` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Scope Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getScopeType` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Scope Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getScopeType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getScopeType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ScopeType getScopeType() {
        return scopeType;
    }

    /**
     * 方法 `getCommandId` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Command Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCommandId` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Command Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCommandId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCommandId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * 方法 `getStatus` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getLastErrorCode` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Last Error Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLastErrorCode` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Last Error Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getLastErrorCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getLastErrorCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getLastErrorCode() {
        return lastErrorCode;
    }

    /**
     * 方法 `getAttempt` 按照 `AuthorizationMutationEntity` 的职责处理输入，完成 `get Attempt` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAttempt` processes its inputs according to `AuthorizationMutationEntity`'s responsibility, performs the `get Attempt` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAttempt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAttempt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getAttempt() {
        return attempt;
    }

    /**
     * 类型 `ScopeType` 位于 `AuthorizationMutationEntity` 内，是枚举，用于承载 `Scope Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ScopeType` is an enum inside `AuthorizationMutationEntity` and carries the responsibility, state, or contract for `Scope Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ScopeType` 作为 `AuthorizationMutationEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ScopeType` as the responsibility boundary of `AuthorizationMutationEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ScopeType {
        /**
         * 字段 `SESSION` 表示 `ScopeType` 中与 `SESSION` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SESSION` stores the `SESSION`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SESSION` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SESSION`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SESSION,
        /**
         * 字段 `USER` 表示 `ScopeType` 中与 `USER` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `USER` stores the `USER`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `USER` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `USER`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        USER,
        /**
         * 字段 `TENANT` 表示 `ScopeType` 中与 `TENANT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `TENANT` stores the `TENANT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `TENANT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `TENANT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        TENANT
    }

    /**
     * 类型 `Status` 位于 `AuthorizationMutationEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `AuthorizationMutationEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `AuthorizationMutationEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `AuthorizationMutationEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `PREPARING` 表示 `Status` 中与 `PREPARING` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PREPARING` stores the `PREPARING`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PREPARING` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PREPARING`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        PREPARING,
        /**
         * 字段 `COMMITTED` 表示 `Status` 中与 `COMMITTED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `COMMITTED` stores the `COMMITTED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `COMMITTED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `COMMITTED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        COMMITTED,
        /**
         * 字段 `PROJECTED` 表示 `Status` 中与 `PROJECTED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PROJECTED` stores the `PROJECTED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PROJECTED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PROJECTED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        PROJECTED,
        /**
         * 字段 `COMPLETED` 表示 `Status` 中与 `COMPLETED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `COMPLETED` stores the `COMPLETED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `COMPLETED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `COMPLETED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        COMPLETED,
        /**
         * 字段 `ABORTED` 表示 `Status` 中与 `ABORTED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ABORTED` stores the `ABORTED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ABORTED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ABORTED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ABORTED,
        /**
         * 字段 `RECOVERY_REQUIRED` 表示 `Status` 中与 `RECOVERY REQUIRED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `RECOVERY_REQUIRED` stores the `RECOVERY REQUIRED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `RECOVERY_REQUIRED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `RECOVERY_REQUIRED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        RECOVERY_REQUIRED
    }
}
