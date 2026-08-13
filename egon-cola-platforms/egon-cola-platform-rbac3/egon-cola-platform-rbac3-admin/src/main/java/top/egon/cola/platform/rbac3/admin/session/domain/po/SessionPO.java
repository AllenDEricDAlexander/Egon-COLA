package top.egon.cola.platform.rbac3.admin.session.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.SessionStatusEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.AuthenticationStrengthEnum;

/**
 * 类型 `SessionPO` 位于当前包内，是类型，用于承载 `Session Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionPO` is a type in its package and carries the responsibility, state, or contract for `Session Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SessionPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SessionPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "SessionEntity")
@Table(name = "rbac3_session")
public class SessionPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `SessionPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `SessionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `userId` 表示 `SessionPO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `SessionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 字段 `sessionId` 表示 `SessionPO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * 字段 `identitySub` 表示 `SessionPO` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `SessionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "identity_sub", nullable = false, length = 128)
    private String identitySub;

    /**
     * 字段 `status` 表示 `SessionPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `SessionStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `SessionPO` (declared type `SessionStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatusEnum status;

    /**
     * 字段 `sessionVersion` 表示 `SessionPO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    /**
     * 字段 `contextVersion` 表示 `SessionPO` 中与 `context Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `contextVersion` stores the `context Version`-related state, dependency, configuration, or result of `SessionPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `contextVersion` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `contextVersion`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "context_version", nullable = false)
    private long contextVersion;

    /**
     * 字段 `authVersionAtIssue` 表示 `SessionPO` 中与 `auth Version At Issue` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authVersionAtIssue` stores the `auth Version At Issue`-related state, dependency, configuration, or result of `SessionPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authVersionAtIssue` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authVersionAtIssue`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "auth_version_at_issue", nullable = false)
    private long authVersionAtIssue;

    /**
     * 字段 `policyVersionAtIssue` 表示 `SessionPO` 中与 `policy Version At Issue` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyVersionAtIssue` stores the `policy Version At Issue`-related state, dependency, configuration, or result of `SessionPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyVersionAtIssue` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyVersionAtIssue`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "policy_version_at_issue", nullable = false)
    private long policyVersionAtIssue;

    /**
     * 字段 `activeRootChecksum` 表示 `SessionPO` 中与 `active Root Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activeRootChecksum` stores the `active Root Checksum`-related state, dependency, configuration, or result of `SessionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activeRootChecksum` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activeRootChecksum`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "active_root_checksum", length = 128)
    private String activeRootChecksum;

    /**
     * 字段 `activationRequired` 表示 `SessionPO` 中与 `activation Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activationRequired` stores the `activation Required`-related state, dependency, configuration, or result of `SessionPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activationRequired` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activationRequired`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "activation_required", nullable = false)
    private boolean activationRequired;

    /**
     * 字段 `tokenFamilyId` 表示 `SessionPO` 中与 `token Family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tokenFamilyId` stores the `token Family Id`-related state, dependency, configuration, or result of `SessionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tokenFamilyId` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tokenFamilyId`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "token_family_id", length = 128)
    private String tokenFamilyId;

    /**
     * 字段 `deviceIdHash` 表示 `SessionPO` 中与 `device Id Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `deviceIdHash` stores the `device Id Hash`-related state, dependency, configuration, or result of `SessionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `deviceIdHash` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `deviceIdHash`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "device_id_hash", length = 128)
    private String deviceIdHash;

    /**
     * 字段 `authenticationStrength` 表示 `SessionPO` 中与 `authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `AuthenticationStrengthEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authenticationStrength` stores the `authentication Strength`-related state, dependency, configuration, or result of `SessionPO` (declared type `AuthenticationStrengthEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authenticationStrength` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authenticationStrength`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_strength", nullable = false, length = 32)
    private AuthenticationStrengthEnum authenticationStrength;

    /**
     * 字段 `authenticatedAt` 表示 `SessionPO` 中与 `authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authenticatedAt` stores the `authenticated At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authenticatedAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authenticatedAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "authenticated_at", nullable = false)
    private Instant authenticatedAt;

    /**
     * 字段 `strongAuthenticatedAt` 表示 `SessionPO` 中与 `strong Authenticated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `strongAuthenticatedAt` stores the `strong Authenticated At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `strongAuthenticatedAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `strongAuthenticatedAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "strong_authenticated_at")
    private Instant strongAuthenticatedAt;

    /**
     * 字段 `lastSeenAt` 表示 `SessionPO` 中与 `last Seen At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastSeenAt` stores the `last Seen At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastSeenAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastSeenAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    /**
     * 字段 `idleExpiresAt` 表示 `SessionPO` 中与 `idle Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idleExpiresAt` stores the `idle Expires At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idleExpiresAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idleExpiresAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    /**
     * 字段 `absoluteExpiresAt` 表示 `SessionPO` 中与 `absolute Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `absoluteExpiresAt` stores the `absolute Expires At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `absoluteExpiresAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `absoluteExpiresAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    /**
     * 字段 `contextExpiresAt` 表示 `SessionPO` 中与 `context Expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `contextExpiresAt` stores the `context Expires At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `contextExpiresAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `contextExpiresAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "context_expires_at", nullable = false)
    private Instant contextExpiresAt;

    /**
     * 字段 `revokedAt` 表示 `SessionPO` 中与 `revoked At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `revokedAt` stores the `revoked At`-related state, dependency, configuration, or result of `SessionPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `revokedAt` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `revokedAt`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * 字段 `revokeReason` 表示 `SessionPO` 中与 `revoke Reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `revokeReason` stores the `revoke Reason`-related state, dependency, configuration, or result of `SessionPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `revokeReason` 时应保持 `SessionPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `revokeReason`, preserve `SessionPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    /**
     * 构造器 `SessionPO` 用于创建并初始化 `SessionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionPO` creates and initializes `SessionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected SessionPO() {
    }

    /**
     * 构造器 `SessionPO` 用于创建并初始化 `SessionPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionPO` creates and initializes `SessionPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tokenFamilyId 输入参数 `tokenFamilyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param deviceIdHash 输入参数 `deviceIdHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authenticationStrength 输入参数 `authenticationStrength`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authenticatedAt 输入参数 `authenticatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idleExpiresAt 输入参数 `idleExpiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param absoluteExpiresAt 输入参数 `absoluteExpiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SessionPO(
            Long id,
            Long tenantId,
            Long userId,
            Long sessionId,
            long authVersion,
            long policyVersion,
            String tokenFamilyId,
            String deviceIdHash,
            AuthenticationStrengthEnum authenticationStrength,
            Instant authenticatedAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt,
            String actorId) {
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        if (!idleExpiresAt.isAfter(authenticatedAt)
                || idleExpiresAt.isAfter(absoluteExpiresAt)) {
            throw new IllegalArgumentException("invalid session expiry window");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.identitySub = userId.toString();
        this.status = SessionStatusEnum.ACTIVE;
        this.authVersionAtIssue = authVersion;
        this.policyVersionAtIssue = policyVersion;
        this.activationRequired = true;
        this.tokenFamilyId = required(tokenFamilyId, "tokenFamilyId");
        this.deviceIdHash = deviceIdHash;
        this.authenticationStrength = Objects.requireNonNull(
                authenticationStrength, "authenticationStrength");
        this.authenticatedAt = Objects.requireNonNull(authenticatedAt, "authenticatedAt");
        this.lastSeenAt = authenticatedAt;
        this.idleExpiresAt = idleExpiresAt;
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt");
        this.contextExpiresAt = absoluteExpiresAt;
        markCreated(actorId, authenticatedAt);
    }

    /**
     * 方法 `authorizationContext` 按照 `SessionPO` 的职责处理输入，完成 `authorization Context` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationContext` processes its inputs according to `SessionPO`'s responsibility, performs the `authorization Context` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationContext` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationContext`, then continue the business flow using its result, exception, or side effect.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param createdAt 输入参数 `createdAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static SessionPO authorizationContext(
            Long id,
            Long tenantId,
            Long userId,
            Long sessionId,
            String identitySub,
            long authVersion,
            long policyVersion,
            Instant createdAt,
            Instant expiresAt,
            String actorId) {
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        if (expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("context expiry must be after creation");
        }
        SessionPO entity = new SessionPO();
        entity.id = Objects.requireNonNull(id, "id");
        entity.setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        entity.userId = Objects.requireNonNull(userId, "userId");
        entity.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        entity.identitySub = required(identitySub, "identitySub");
        entity.status = SessionStatusEnum.ACTIVE;
        entity.authVersionAtIssue = authVersion;
        entity.policyVersionAtIssue = policyVersion;
        entity.activationRequired = true;
        entity.authenticationStrength = AuthenticationStrengthEnum.STRONG;
        entity.authenticatedAt = Objects.requireNonNull(createdAt, "createdAt");
        entity.strongAuthenticatedAt = createdAt;
        entity.lastSeenAt = createdAt;
        entity.idleExpiresAt = expiresAt;
        entity.absoluteExpiresAt = expiresAt;
        entity.contextExpiresAt = expiresAt;
        entity.markCreated(actorId, createdAt);
        return entity;
    }

    /**
     * 方法 `refresh` 按照 `SessionPO` 的职责处理输入，完成 `refresh` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `refresh` processes its inputs according to `SessionPO`'s responsibility, performs the `refresh` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `refresh` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `refresh`, then continue the business flow using its result, exception, or side effect.
     *
     * @param currentPolicyVersion 输入参数 `currentPolicyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param nextIdleExpiry 输入参数 `nextIdleExpiry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void refresh(long currentPolicyVersion, Instant now, Instant nextIdleExpiry, String actorId) {
        requireActive(now);
        if (currentPolicyVersion < 0) {
            throw new IllegalArgumentException("currentPolicyVersion must not be negative");
        }
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        policyVersionAtIssue = currentPolicyVersion;
        lastSeenAt = now;
        idleExpiresAt = nextIdleExpiry.isAfter(absoluteExpiresAt)
                ? absoluteExpiresAt
                : nextIdleExpiry;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `activateRoles` 按照 `SessionPO` 的职责处理输入，完成 `activate Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activateRoles` processes its inputs according to `SessionPO`'s responsibility, performs the `activate Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activateRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activateRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param currentAuthVersion 输入参数 `currentAuthVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param currentPolicyVersion 输入参数 `currentPolicyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rootChecksum 输入参数 `rootChecksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activateRoles(
            long currentAuthVersion,
            long currentPolicyVersion,
            String rootChecksum,
            String actorId,
            Instant now
    ) {
        requireActive(now);
        if (currentAuthVersion < 0 || currentPolicyVersion < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        authVersionAtIssue = currentAuthVersion;
        policyVersionAtIssue = currentPolicyVersion;
        activeRootChecksum = required(rootChecksum, "rootChecksum");
        activationRequired = false;
        lastSeenAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `requireRoleReselection` 按照 `SessionPO` 的职责处理输入，完成 `require Role Reselection` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireRoleReselection` processes its inputs according to `SessionPO`'s responsibility, performs the `require Role Reselection` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireRoleReselection` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireRoleReselection`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void requireRoleReselection(String actorId, Instant now) {
        requireActive(now);
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        activeRootChecksum = null;
        activationRequired = true;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `logout` 按照 `SessionPO` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `logout` processes its inputs according to `SessionPO`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `logout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `logout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean logout(String actorId, Instant now) {
        if (status == SessionStatusEnum.LOGGED_OUT) {
            return false;
        }
        if (status != SessionStatusEnum.ACTIVE) {
            return false;
        }
        status = SessionStatusEnum.LOGGED_OUT;
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        revokedAt = now;
        revokeReason = "USER_LOGOUT";
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `compromise` 按照 `SessionPO` 的职责处理输入，完成 `compromise` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `compromise` processes its inputs according to `SessionPO`'s responsibility, performs the `compromise` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `compromise` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `compromise`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean compromise(Instant now, String actorId) {
        if (status != SessionStatusEnum.ACTIVE) {
            return false;
        }
        status = SessionStatusEnum.COMPROMISED;
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        revokedAt = now;
        revokeReason = "REFRESH_TOKEN_REUSED";
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `revoke` 按照 `SessionPO` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `SessionPO`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean revoke(String reason, String actorId, Instant now) {
        if (status != SessionStatusEnum.ACTIVE) {
            return false;
        }
        status = SessionStatusEnum.REVOKED;
        sessionVersion = Math.incrementExact(sessionVersion);
        contextVersion = Math.incrementExact(contextVersion);
        revokedAt = now;
        revokeReason = required(reason, "reason");
        markUpdated(actorId, now);
        return true;
    }

    /**
     * 方法 `stepUp` 按照 `SessionPO` 的职责处理输入，完成 `step Up` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stepUp` processes its inputs according to `SessionPO`'s responsibility, performs the `step Up` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stepUp` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stepUp`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void stepUp(String actorId, Instant now) {
        requireActive(now);
        authenticationStrength = AuthenticationStrengthEnum.STRONG;
        strongAuthenticatedAt = now;
        lastSeenAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `isStrongAuthenticationRecent` 按照 `SessionPO` 的职责处理输入，完成 `is Strong Authentication Recent` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isStrongAuthenticationRecent` processes its inputs according to `SessionPO`'s responsibility, performs the `is Strong Authentication Recent` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isStrongAuthenticationRecent` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isStrongAuthenticationRecent`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumAge 输入参数 `maximumAge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isStrongAuthenticationRecent(Instant now, Duration maximumAge) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(maximumAge, "maximumAge");
        return authenticationStrength == AuthenticationStrengthEnum.STRONG
                && strongAuthenticatedAt != null
                && strongAuthenticatedAt.plus(maximumAge).isAfter(now);
    }

    /**
     * 方法 `requireActive` 按照 `SessionPO` 的职责处理输入，完成 `require Active` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireActive` processes its inputs according to `SessionPO`'s responsibility, performs the `require Active` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireActive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireActive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void requireActive(Instant now) {
        if (status != SessionStatusEnum.ACTIVE) {
            throw new IllegalStateException("session is not active");
        }
        if (!idleExpiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now)
                || !contextExpiresAt.isAfter(now)) {
            throw new IllegalStateException("session has expired");
        }
    }

    /**
     * 方法 `getSessionId` 按照 `SessionPO` 的职责处理输入，完成 `get Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionId` processes its inputs according to `SessionPO`'s responsibility, performs the `get Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getId` 按照 `SessionPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `SessionPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getIdentitySub` 按照 `SessionPO` 的职责处理输入，完成 `get Identity Sub` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getIdentitySub` processes its inputs according to `SessionPO`'s responsibility, performs the `get Identity Sub` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getIdentitySub` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getIdentitySub`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getIdentitySub() {
        return identitySub;
    }

    /**
     * 方法 `getUserId` 按照 `SessionPO` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `SessionPO`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `SessionPO` 的职责处理输入，完成 `get SessionStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `SessionPO`'s responsibility, performs the `get SessionStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SessionStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getSessionVersion` 按照 `SessionPO` 的职责处理输入，完成 `get Session Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionVersion` processes its inputs according to `SessionPO`'s responsibility, performs the `get Session Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSessionVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSessionVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getSessionVersion() {
        return sessionVersion;
    }

    /**
     * 方法 `getContextVersion` 按照 `SessionPO` 的职责处理输入，完成 `get Context Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getContextVersion` processes its inputs according to `SessionPO`'s responsibility, performs the `get Context Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getContextVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getContextVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getContextVersion() {
        return contextVersion;
    }

    /**
     * 方法 `getAuthVersionAtIssue` 按照 `SessionPO` 的职责处理输入，完成 `get Auth Version At Issue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAuthVersionAtIssue` processes its inputs according to `SessionPO`'s responsibility, performs the `get Auth Version At Issue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAuthVersionAtIssue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAuthVersionAtIssue`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getAuthVersionAtIssue() {
        return authVersionAtIssue;
    }

    /**
     * 方法 `getPolicyVersionAtIssue` 按照 `SessionPO` 的职责处理输入，完成 `get Policy Version At Issue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPolicyVersionAtIssue` processes its inputs according to `SessionPO`'s responsibility, performs the `get Policy Version At Issue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPolicyVersionAtIssue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPolicyVersionAtIssue`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getPolicyVersionAtIssue() {
        return policyVersionAtIssue;
    }

    /**
     * 方法 `getActiveRootChecksum` 按照 `SessionPO` 的职责处理输入，完成 `get Active Root Checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActiveRootChecksum` processes its inputs according to `SessionPO`'s responsibility, performs the `get Active Root Checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActiveRootChecksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActiveRootChecksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getActiveRootChecksum() {
        return activeRootChecksum;
    }

    /**
     * 方法 `isActivationRequired` 按照 `SessionPO` 的职责处理输入，完成 `is Activation Required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isActivationRequired` processes its inputs according to `SessionPO`'s responsibility, performs the `is Activation Required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isActivationRequired` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isActivationRequired`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isActivationRequired() {
        return activationRequired;
    }

    /**
     * 方法 `getAuthenticationStrength` 按照 `SessionPO` 的职责处理输入，完成 `get Authentication Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAuthenticationStrength` processes its inputs according to `SessionPO`'s responsibility, performs the `get Authentication Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAuthenticationStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAuthenticationStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthenticationStrengthEnum getAuthenticationStrength() {
        return authenticationStrength;
    }

    /**
     * 方法 `getLastSeenAt` 按照 `SessionPO` 的职责处理输入，完成 `get Last Seen At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLastSeenAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Last Seen At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getLastSeenAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getLastSeenAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    /**
     * 方法 `getAuthenticatedAt` 按照 `SessionPO` 的职责处理输入，完成 `get Authenticated At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAuthenticatedAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Authenticated At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAuthenticatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAuthenticatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    /**
     * 方法 `getStrongAuthenticatedAt` 按照 `SessionPO` 的职责处理输入，完成 `get Strong Authenticated At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStrongAuthenticatedAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Strong Authenticated At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStrongAuthenticatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStrongAuthenticatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getStrongAuthenticatedAt() {
        return strongAuthenticatedAt;
    }

    /**
     * 方法 `getIdleExpiresAt` 按照 `SessionPO` 的职责处理输入，完成 `get Idle Expires At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getIdleExpiresAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Idle Expires At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getIdleExpiresAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getIdleExpiresAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getIdleExpiresAt() {
        return idleExpiresAt;
    }

    /**
     * 方法 `getContextExpiresAt` 按照 `SessionPO` 的职责处理输入，完成 `get Context Expires At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getContextExpiresAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Context Expires At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getContextExpiresAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getContextExpiresAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getContextExpiresAt() {
        return contextExpiresAt;
    }

    /**
     * 方法 `getTokenFamilyId` 按照 `SessionPO` 的职责处理输入，完成 `get Token Family Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTokenFamilyId` processes its inputs according to `SessionPO`'s responsibility, performs the `get Token Family Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTokenFamilyId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTokenFamilyId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getTokenFamilyId() {
        return tokenFamilyId;
    }

    /**
     * 方法 `getAbsoluteExpiresAt` 按照 `SessionPO` 的职责处理输入，完成 `get Absolute Expires At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAbsoluteExpiresAt` processes its inputs according to `SessionPO`'s responsibility, performs the `get Absolute Expires At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAbsoluteExpiresAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAbsoluteExpiresAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getAbsoluteExpiresAt() {
        return absoluteExpiresAt;
    }

    /**
     * 方法 `getRevokeReason` 按照 `SessionPO` 的职责处理输入，完成 `get Revoke Reason` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRevokeReason` processes its inputs according to `SessionPO`'s responsibility, performs the `get Revoke Reason` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRevokeReason` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRevokeReason`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getRevokeReason() {
        return revokeReason;
    }

    /**
     * 方法 `required` 按照 `SessionPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `SessionPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
