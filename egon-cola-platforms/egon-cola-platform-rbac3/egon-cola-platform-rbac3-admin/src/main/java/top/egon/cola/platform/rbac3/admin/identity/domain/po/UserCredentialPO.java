package top.egon.cola.platform.rbac3.admin.identity.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialTypeEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialStatusEnum;

/**
 * 类型 `UserCredentialPO` 位于当前包内，是类型，用于承载 `User Credential Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `UserCredentialPO` is a type in its package and carries the responsibility, state, or contract for `User Credential Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `UserCredentialPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `UserCredentialPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "UserCredentialEntity")
@Table(name = "rbac3_user_credential")
public class UserCredentialPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `UserCredentialPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `userId` 表示 `UserCredentialPO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 字段 `credentialType` 表示 `UserCredentialPO` 中与 `credential Type` 相关的状态、依赖、配置或结果（声明类型 `UserCredentialTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialType` stores the `credential Type`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `UserCredentialTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialType` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialType`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private UserCredentialTypeEnum credentialType;

    /**
     * 字段 `passwordHash` 表示 `UserCredentialPO` 中与 `password Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `passwordHash` stores the `password Hash`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `passwordHash` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `passwordHash`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "password_hash", length = 512)
    private String passwordHash;

    /**
     * 字段 `credentialVersion` 表示 `UserCredentialPO` 中与 `credential Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialVersion` stores the `credential Version`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialVersion` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialVersion`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    /**
     * 字段 `mustChangePassword` 表示 `UserCredentialPO` 中与 `must Change Password` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `mustChangePassword` stores the `must Change Password`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `mustChangePassword` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `mustChangePassword`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /**
     * 字段 `failedAttempts` 表示 `UserCredentialPO` 中与 `failed Attempts` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `failedAttempts` stores the `failed Attempts`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `failedAttempts` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `failedAttempts`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /**
     * 字段 `lockedUntil` 表示 `UserCredentialPO` 中与 `locked Until` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lockedUntil` stores the `locked Until`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lockedUntil` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lockedUntil`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * 字段 `passwordChangedAt` 表示 `UserCredentialPO` 中与 `password Changed At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `passwordChangedAt` stores the `password Changed At`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `passwordChangedAt` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `passwordChangedAt`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /**
     * 字段 `status` 表示 `UserCredentialPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `UserCredentialStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `UserCredentialPO` (declared type `UserCredentialStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `UserCredentialPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `UserCredentialPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserCredentialStatusEnum status;

    /**
     * 构造器 `UserCredentialPO` 用于创建并初始化 `UserCredentialPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserCredentialPO` creates and initializes `UserCredentialPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserCredentialPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserCredentialPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected UserCredentialPO() {
    }

    /**
     * 构造器 `UserCredentialPO` 用于创建并初始化 `UserCredentialPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `UserCredentialPO` creates and initializes `UserCredentialPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `UserCredentialPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `UserCredentialPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param passwordHash 输入参数 `passwordHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mustChangePassword 输入参数 `mustChangePassword`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public UserCredentialPO(
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
        this.credentialType = UserCredentialTypeEnum.PASSWORD;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.mustChangePassword = mustChangePassword;
        this.passwordChangedAt = Objects.requireNonNull(now, "now");
        this.status = UserCredentialStatusEnum.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `recordFailure` 按照 `UserCredentialPO` 的职责处理输入，完成 `record Failure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordFailure` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `record Failure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        status = until == null ? UserCredentialStatusEnum.ACTIVE : UserCredentialStatusEnum.LOCKED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `recordSuccess` 按照 `UserCredentialPO` 的职责处理输入，完成 `record Success` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `recordSuccess` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `record Success` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        status = UserCredentialStatusEnum.ACTIVE;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `replacePasswordHash` 按照 `UserCredentialPO` 的职责处理输入，完成 `replace Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `replacePasswordHash` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `replace Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getUserId` 按照 `UserCredentialPO` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getPasswordHash` 按照 `UserCredentialPO` 的职责处理输入，完成 `get Password Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPasswordHash` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `get Password Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getFailedAttempts` 按照 `UserCredentialPO` 的职责处理输入，完成 `get Failed Attempts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getFailedAttempts` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `get Failed Attempts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getLockedUntil` 按照 `UserCredentialPO` 的职责处理输入，完成 `get Locked Until` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLockedUntil` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `get Locked Until` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `UserCredentialPO` 的职责处理输入，完成 `get UserCredentialStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `UserCredentialPO`'s responsibility, performs the `get UserCredentialStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public UserCredentialStatusEnum getStatus() {
        return status;
    }


    }
