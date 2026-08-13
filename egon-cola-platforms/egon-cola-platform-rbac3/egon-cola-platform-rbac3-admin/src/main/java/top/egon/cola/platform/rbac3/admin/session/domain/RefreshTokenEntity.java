package top.egon.cola.platform.rbac3.admin.session.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `RefreshTokenEntity` 位于当前包内，是类型，用于承载 `Refresh Token Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RefreshTokenEntity` is a type in its package and carries the responsibility, state, or contract for `Refresh Token Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RefreshTokenEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RefreshTokenEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_refresh_token")
public class RefreshTokenEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `RefreshTokenEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `sessionId` 表示 `RefreshTokenEntity` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * 字段 `familyId` 表示 `RefreshTokenEntity` 中与 `family Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `familyId` stores the `family Id`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `familyId` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `familyId`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "family_id", nullable = false, length = 128)
    private String familyId;

    /**
     * 字段 `generation` 表示 `RefreshTokenEntity` 中与 `generation` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `generation` stores the `generation`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `generation` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `generation`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private long generation;

    /**
     * 字段 `tokenHash` 表示 `RefreshTokenEntity` 中与 `token Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tokenHash` stores the `token Hash`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tokenHash` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tokenHash`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "token_hash", nullable = false, length = 256, unique = true)
    private String tokenHash;

    /**
     * 字段 `status` 表示 `RefreshTokenEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 字段 `issuedAt` 表示 `RefreshTokenEntity` 中与 `issued At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `issuedAt` stores the `issued At`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `issuedAt` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `issuedAt`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /**
     * 字段 `expiresAt` 表示 `RefreshTokenEntity` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 字段 `rotatedAt` 表示 `RefreshTokenEntity` 中与 `rotated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `rotatedAt` stores the `rotated At`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `rotatedAt` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `rotatedAt`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "rotated_at")
    private Instant rotatedAt;

    /**
     * 字段 `replacedById` 表示 `RefreshTokenEntity` 中与 `replaced By Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `replacedById` stores the `replaced By Id`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `replacedById` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `replacedById`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "replaced_by_id")
    private Long replacedById;

    /**
     * 字段 `reuseDetectedAt` 表示 `RefreshTokenEntity` 中与 `reuse Detected At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `reuseDetectedAt` stores the `reuse Detected At`-related state, dependency, configuration, or result of `RefreshTokenEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `reuseDetectedAt` 时应保持 `RefreshTokenEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `reuseDetectedAt`, preserve `RefreshTokenEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "reuse_detected_at")
    private Instant reuseDetectedAt;

    /**
     * 构造器 `RefreshTokenEntity` 用于创建并初始化 `RefreshTokenEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshTokenEntity` creates and initializes `RefreshTokenEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshTokenEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshTokenEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RefreshTokenEntity() {
    }

    /**
     * 构造器 `RefreshTokenEntity` 用于创建并初始化 `RefreshTokenEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RefreshTokenEntity` creates and initializes `RefreshTokenEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RefreshTokenEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RefreshTokenEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param familyId 输入参数 `familyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param generation 输入参数 `generation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tokenHash 输入参数 `tokenHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param issuedAt 输入参数 `issuedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RefreshTokenEntity(
            Long id,
            Long tenantId,
            Long sessionId,
            String familyId,
            long generation,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String actorId) {
        if (generation < 0) {
            throw new IllegalArgumentException("generation must not be negative");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.familyId = required(familyId, "familyId");
        this.generation = generation;
        this.tokenHash = required(tokenHash, "tokenHash");
        this.status = Status.ACTIVE;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        markCreated(actorId, issuedAt);
    }

    /**
     * 方法 `rotate` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `rotate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rotate` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `rotate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rotate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rotate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextTokenId 输入参数 `nextTokenId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void rotate(Long nextTokenId, Instant now, String actorId) {
        requireActive();
        status = Status.ROTATED;
        rotatedAt = now;
        replacedById = Objects.requireNonNull(nextTokenId, "nextTokenId");
        markUpdated(actorId, now);
    }

    /**
     * 方法 `markReused` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `mark Reused` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `markReused` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `mark Reused` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `markReused` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `markReused`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void markReused(Instant now, String actorId) {
        if (status != Status.ROTATED && status != Status.REUSED_DETECTED) {
            throw new IllegalStateException("only rotated token can be marked reused");
        }
        status = Status.REUSED_DETECTED;
        reuseDetectedAt = now;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `revoke` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `revoke` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `revoke` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `revoke` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `revoke` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `revoke`, then continue the business flow using its result, exception, or side effect.
     *
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void revoke(Instant now, String actorId) {
        if (status == Status.ACTIVE) {
            status = Status.REVOKED;
            markUpdated(actorId, now);
        }
    }

    /**
     * 方法 `requireActive` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `require Active` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireActive` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `require Active` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireActive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireActive`, then continue the business flow using its result, exception, or side effect.
     */
    public void requireActive() {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("refresh token is not active");
        }
    }

    /**
     * 方法 `getId` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSessionId` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionId` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getFamilyId` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Family Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getFamilyId` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Family Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getFamilyId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getFamilyId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getFamilyId() {
        return familyId;
    }

    /**
     * 方法 `getGeneration` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Generation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getGeneration` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Generation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getGeneration` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getGeneration`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getGeneration() {
        return generation;
    }

    /**
     * 方法 `getTokenHash` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Token Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTokenHash` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Token Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTokenHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTokenHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getTokenHash() {
        return tokenHash;
    }

    /**
     * 方法 `getStatus` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getExpiresAt` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `get Expires At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getExpiresAt` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `get Expires At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getExpiresAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getExpiresAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 方法 `required` 按照 `RefreshTokenEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `RefreshTokenEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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

    /**
     * 类型 `Status` 位于 `RefreshTokenEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `RefreshTokenEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `RefreshTokenEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `RefreshTokenEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
         * 字段 `ROTATED` 表示 `Status` 中与 `ROTATED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROTATED` stores the `ROTATED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROTATED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROTATED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROTATED,
        /**
         * 字段 `REUSED_DETECTED` 表示 `Status` 中与 `REUSED DETECTED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REUSED_DETECTED` stores the `REUSED DETECTED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REUSED_DETECTED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REUSED_DETECTED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        REUSED_DETECTED,
        /**
         * 字段 `REVOKED` 表示 `Status` 中与 `REVOKED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKED` stores the `REVOKED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKED,
        /**
         * 字段 `EXPIRED` 表示 `Status` 中与 `EXPIRED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `EXPIRED` stores the `EXPIRED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `EXPIRED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `EXPIRED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        EXPIRED
    }
}
