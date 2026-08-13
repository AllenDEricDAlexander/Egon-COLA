package top.egon.cola.platform.rbac3.admin.identity.domain;

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
 * 类型 `UserCredentialEntity` 位于当前包内，是类型，用于承载 `User Credential Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `UserCredentialEntity` is a type in its package and carries the responsibility, state, or contract for `User Credential Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `UserCredentialEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `UserCredentialEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_user_credential")
public class UserCredentialEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `UserCredentialEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `userId` 表示 `UserCredentialEntity` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 字段 `credentialType` 表示 `UserCredentialEntity` 中与 `credential Type` 相关的状态、依赖、配置或结果（声明类型 `CredentialType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialType` stores the `credential Type`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `CredentialType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialType` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialType`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private CredentialType credentialType;

    /**
     * 字段 `passwordHash` 表示 `UserCredentialEntity` 中与 `password Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `passwordHash` stores the `password Hash`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `passwordHash` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `passwordHash`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "password_hash", length = 512)
    private String passwordHash;

    /**
     * 字段 `credentialVersion` 表示 `UserCredentialEntity` 中与 `credential Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialVersion` stores the `credential Version`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialVersion` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialVersion`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    /**
     * 字段 `mustChangePassword` 表示 `UserCredentialEntity` 中与 `must Change Password` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mustChangePassword` stores the `must Change Password`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mustChangePassword` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mustChangePassword`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /**
     * 字段 `failedAttempts` 表示 `UserCredentialEntity` 中与 `failed Attempts` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `failedAttempts` stores the `failed Attempts`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `failedAttempts` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `failedAttempts`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /**
     * 字段 `lockedUntil` 表示 `UserCredentialEntity` 中与 `locked Until` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lockedUntil` stores the `locked Until`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lockedUntil` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lockedUntil`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * 字段 `passwordChangedAt` 表示 `UserCredentialEntity` 中与 `password Changed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `passwordChangedAt` stores the `password Changed At`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `passwordChangedAt` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `passwordChangedAt`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /**
     * 字段 `status` 表示 `UserCredentialEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserCredentialEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `UserCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `UserCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 构造器 `UserCredentialEntity` 用于创建并初始化 `UserCredentialEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserCredentialEntity` creates and initializes `UserCredentialEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserCredentialEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserCredentialEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected UserCredentialEntity() {
    }

    /**
     * 构造器 `UserCredentialEntity` 用于创建并初始化 `UserCredentialEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserCredentialEntity` creates and initializes `UserCredentialEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserCredentialEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserCredentialEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param passwordHash 输入参数 `passwordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mustChangePassword 输入参数 `mustChangePassword`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserCredentialEntity(
            Long id,
            Long tenantId,
            Long userId,
            String passwordHash,
            boolean mustChangePassword,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.credentialType = CredentialType.PASSWORD;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.mustChangePassword = mustChangePassword;
        this.passwordChangedAt = Objects.requireNonNull(now, "now");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `recordFailure` 按照 `UserCredentialEntity` 的职责处理输入，完成 `record Failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordFailure` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `record Failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recordFailure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recordFailure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param attempts 输入参数 `attempts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param until 输入参数 `until`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void recordFailure(int attempts, Instant until, String actorId, Instant now) {
        failedAttempts = attempts;
        lockedUntil = until;
        status = until == null ? Status.ACTIVE : Status.LOCKED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `recordSuccess` 按照 `UserCredentialEntity` 的职责处理输入，完成 `record Success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordSuccess` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `record Success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `recordSuccess` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `recordSuccess`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void recordSuccess(String actorId, Instant now) {
        failedAttempts = 0;
        lockedUntil = null;
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `replacePasswordHash` 按照 `UserCredentialEntity` 的职责处理输入，完成 `replace Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `replacePasswordHash` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `replace Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `replacePasswordHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `replacePasswordHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextPasswordHash 输入参数 `nextPasswordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void replacePasswordHash(String nextPasswordHash, String actorId, Instant now) {
        passwordHash = Objects.requireNonNull(nextPasswordHash, "nextPasswordHash");
        credentialVersion = Math.incrementExact(credentialVersion);
        passwordChangedAt = Objects.requireNonNull(now, "now");
        mustChangePassword = false;
        recordSuccess(actorId, now);
    }

    /**
     * 方法 `getUserId` 按照 `UserCredentialEntity` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getPasswordHash` 按照 `UserCredentialEntity` 的职责处理输入，完成 `get Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPasswordHash` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `get Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPasswordHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPasswordHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 方法 `getFailedAttempts` 按照 `UserCredentialEntity` 的职责处理输入，完成 `get Failed Attempts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getFailedAttempts` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `get Failed Attempts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getFailedAttempts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getFailedAttempts`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * 方法 `getLockedUntil` 按照 `UserCredentialEntity` 的职责处理输入，完成 `get Locked Until` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLockedUntil` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `get Locked Until` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getLockedUntil` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getLockedUntil`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /**
     * 方法 `getStatus` 按照 `UserCredentialEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `UserCredentialEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `CredentialType` 位于 `UserCredentialEntity` 内，是枚举，用于承载 `Credential Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CredentialType` is an enum inside `UserCredentialEntity` and carries the responsibility, state, or contract for `Credential Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CredentialType` 作为 `UserCredentialEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CredentialType` as the responsibility boundary of `UserCredentialEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum CredentialType {
        /**
         * 字段 `PASSWORD` 表示 `CredentialType` 中与 `PASSWORD` 相关的状态、依赖、配置或结果（声明类型 `CredentialType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PASSWORD` stores the `PASSWORD`-related state, dependency, configuration, or result of `CredentialType` (declared type `CredentialType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PASSWORD` 时应保持 `CredentialType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PASSWORD`, preserve `CredentialType`'s lifecycle, immutability, and thread-safety constraints.
         */
        PASSWORD
    }

    /**
     * 类型 `Status` 位于 `UserCredentialEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `UserCredentialEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `UserCredentialEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `UserCredentialEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
         * 字段 `LOCKED` 表示 `Status` 中与 `LOCKED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `LOCKED` stores the `LOCKED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `LOCKED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `LOCKED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        LOCKED,
        /**
         * 字段 `DISABLED` 表示 `Status` 中与 `DISABLED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DISABLED` stores the `DISABLED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DISABLED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DISABLED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        DISABLED,
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
