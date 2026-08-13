package top.egon.cola.platform.rbac3.admin.runtime.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationStatusEnum;

/**
 * 类型 `AuthorizationMutationPO` 位于当前包内，是类型，用于承载 `Authorization Mutation Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationMutationPO` is a type in its package and carries the responsibility, state, or contract for `Authorization Mutation Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuthorizationMutationPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthorizationMutationPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "AuthorizationMutationEntity")
@Table(name = "rbac3_authorization_mutation")
public class AuthorizationMutationPO extends TenantScopedPO {

    /**
     * 字段 `mutationId` 表示 `AuthorizationMutationPO` 中与 `mutation Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mutationId` stores the `mutation Id`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mutationId` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mutationId`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "mutation_id")
    private Long mutationId;
    /**
     * 字段 `userId` 表示 `AuthorizationMutationPO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id")
    private Long userId;
    /**
     * 字段 `sessionId` 表示 `AuthorizationMutationPO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_id")
    private Long sessionId;
    /**
     * 字段 `scopeType` 表示 `AuthorizationMutationPO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationScopeTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `AuthorizationMutationScopeTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private AuthorizationMutationScopeTypeEnum scopeType;
    /**
     * 字段 `commandId` 表示 `AuthorizationMutationPO` 中与 `command Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `commandId` stores the `command Id`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `commandId` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `commandId`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;
    /**
     * 字段 `status` 表示 `AuthorizationMutationPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationMutationStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `AuthorizationMutationStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthorizationMutationStatusEnum status;
    /**
     * 字段 `oldSessionVersion` 表示 `AuthorizationMutationPO` 中与 `old Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldSessionVersion` stores the `old Session Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldSessionVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldSessionVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_session_version")
    private Long oldSessionVersion;
    /**
     * 字段 `newSessionVersion` 表示 `AuthorizationMutationPO` 中与 `new Session Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newSessionVersion` stores the `new Session Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newSessionVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newSessionVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_session_version")
    private Long newSessionVersion;
    /**
     * 字段 `oldAuthVersion` 表示 `AuthorizationMutationPO` 中与 `old Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldAuthVersion` stores the `old Auth Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldAuthVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldAuthVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_auth_version")
    private Long oldAuthVersion;
    /**
     * 字段 `newAuthVersion` 表示 `AuthorizationMutationPO` 中与 `new Auth Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newAuthVersion` stores the `new Auth Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newAuthVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newAuthVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_auth_version")
    private Long newAuthVersion;
    /**
     * 字段 `oldPolicyVersion` 表示 `AuthorizationMutationPO` 中与 `old Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `oldPolicyVersion` stores the `old Policy Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `oldPolicyVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `oldPolicyVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "old_policy_version")
    private Long oldPolicyVersion;
    /**
     * 字段 `newPolicyVersion` 表示 `AuthorizationMutationPO` 中与 `new Policy Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `newPolicyVersion` stores the `new Policy Version`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `newPolicyVersion` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `newPolicyVersion`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "new_policy_version")
    private Long newPolicyVersion;
    /**
     * 字段 `fenceCreatedAt` 表示 `AuthorizationMutationPO` 中与 `fence Created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fenceCreatedAt` stores the `fence Created At`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fenceCreatedAt` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fenceCreatedAt`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "fence_created_at")
    private Instant fenceCreatedAt;
    /**
     * 字段 `committedAt` 表示 `AuthorizationMutationPO` 中与 `committed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `committedAt` stores the `committed At`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `committedAt` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `committedAt`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "committed_at")
    private Instant committedAt;
    /**
     * 字段 `projectedAt` 表示 `AuthorizationMutationPO` 中与 `projected At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `projectedAt` stores the `projected At`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `projectedAt` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `projectedAt`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "projected_at")
    private Instant projectedAt;
    /**
     * 字段 `completedAt` 表示 `AuthorizationMutationPO` 中与 `completed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `completedAt` stores the `completed At`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `completedAt` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `completedAt`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    /**
     * 字段 `lastErrorCode` 表示 `AuthorizationMutationPO` 中与 `last Error Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastErrorCode` stores the `last Error Code`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastErrorCode` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastErrorCode`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "last_error_code", length = 128)
    private String lastErrorCode;
    /**
     * 字段 `attempt` 表示 `AuthorizationMutationPO` 中与 `attempt` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `attempt` stores the `attempt`-related state, dependency, configuration, or result of `AuthorizationMutationPO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `attempt` 时应保持 `AuthorizationMutationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `attempt`, preserve `AuthorizationMutationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private int attempt;

    /**
     * 构造器 `AuthorizationMutationPO` 用于创建并初始化 `AuthorizationMutationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationPO` creates and initializes `AuthorizationMutationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected AuthorizationMutationPO() {
    }

    /**
     * 构造器 `AuthorizationMutationPO` 用于创建并初始化 `AuthorizationMutationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthorizationMutationPO` creates and initializes `AuthorizationMutationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthorizationMutationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthorizationMutationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
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
    public AuthorizationMutationPO(
            Long mutationId,
            Long tenantId,
            Long userId,
            Long sessionId,
            AuthorizationMutationScopeTypeEnum scopeType,
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
        this.status = AuthorizationMutationStatusEnum.PREPARING;
        this.oldSessionVersion = oldSessionVersion;
        this.newSessionVersion = newSessionVersion;
        this.oldAuthVersion = oldAuthVersion;
        this.newAuthVersion = newAuthVersion;
        this.oldPolicyVersion = oldPolicyVersion;
        this.newPolicyVersion = newPolicyVersion;
        markCreated(actorId, now);
    }

    /**
     * 方法 `committed` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `committed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `committed` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `committed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `committed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `committed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void committed(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.COMMITTED;
        committedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `fenced` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fenced` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `projected` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `projected` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `projected` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `projected` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `projected` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `projected`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void projected(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.PROJECTED;
        projectedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `completed` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `completed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `completed` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `completed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `completed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `completed`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void completed(Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.COMPLETED;
        completedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `recoveryRequired` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `recovery Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recoveryRequired` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `recovery Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recoveryRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recoveryRequired`, then continue the business flow using its result, exception, or side effect.
     *
     * @param errorCode 输入参数 `errorCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void recoveryRequired(String errorCode, Instant now, String actorId) {
        status = AuthorizationMutationStatusEnum.RECOVERY_REQUIRED;
        lastErrorCode = errorCode;
        attempt = Math.incrementExact(attempt);
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getMutationId` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Mutation Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getMutationId` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Mutation Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getUserId` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSessionId` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionId` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getScopeType` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Scope Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getScopeType` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Scope Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getScopeType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getScopeType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthorizationMutationScopeTypeEnum getScopeType() {
        return scopeType;
    }

    /**
     * 方法 `getCommandId` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Command Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCommandId` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Command Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get AuthorizationMutationStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get AuthorizationMutationStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthorizationMutationStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getLastErrorCode` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Last Error Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLastErrorCode` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Last Error Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getAttempt` 按照 `AuthorizationMutationPO` 的职责处理输入，完成 `get Attempt` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAttempt` processes its inputs according to `AuthorizationMutationPO`'s responsibility, performs the `get Attempt` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAttempt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAttempt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getAttempt() {
        return attempt;
    }


    }
