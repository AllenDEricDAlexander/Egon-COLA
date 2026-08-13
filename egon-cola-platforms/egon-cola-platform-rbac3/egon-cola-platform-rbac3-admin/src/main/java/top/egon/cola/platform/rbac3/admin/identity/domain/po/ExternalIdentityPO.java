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
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.ExternalIdentityStatusEnum;

/**
 * 类型 `ExternalIdentityPO` 位于当前包内，是类型，用于承载 `External Identity Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ExternalIdentityPO` is a type in its package and carries the responsibility, state, or contract for `External Identity Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ExternalIdentityPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ExternalIdentityPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ExternalIdentityEntity")
@Table(name = "rbac3_external_identity")
public class ExternalIdentityPO extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `ExternalIdentityPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `providerCode` 表示 `ExternalIdentityPO` 中与 `provider Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerCode` stores the `provider Code`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerCode` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerCode`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "provider_code", nullable = false, length = 128)
    private String providerCode;

    /**
     * 字段 `externalSubjectId` 表示 `ExternalIdentityPO` 中与 `external Subject Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `externalSubjectId` stores the `external Subject Id`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `externalSubjectId` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `externalSubjectId`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "external_subject_id", nullable = false, length = 256)
    private String externalSubjectId;

    /**
     * 字段 `identitySub` 表示 `ExternalIdentityPO` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "identity_sub", nullable = false, length = 512)
    private String identitySub;

    /**
     * 字段 `userId` 表示 `ExternalIdentityPO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userId` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userId`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 字段 `status` 表示 `ExternalIdentityPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `ExternalIdentityStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `ExternalIdentityStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExternalIdentityStatusEnum status;

    /**
     * 字段 `syncVersion` 表示 `ExternalIdentityPO` 中与 `sync Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `syncVersion` stores the `sync Version`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `syncVersion` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `syncVersion`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "sync_version", nullable = false)
    private long syncVersion;

    /**
     * 字段 `lastSyncedAt` 表示 `ExternalIdentityPO` 中与 `last Synced At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastSyncedAt` stores the `last Synced At`-related state, dependency, configuration, or result of `ExternalIdentityPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastSyncedAt` 时应保持 `ExternalIdentityPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastSyncedAt`, preserve `ExternalIdentityPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /**
     * 构造器 `ExternalIdentityPO` 用于创建并初始化 `ExternalIdentityPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ExternalIdentityPO` creates and initializes `ExternalIdentityPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ExternalIdentityPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ExternalIdentityPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ExternalIdentityPO() {
    }

    /**
     * 构造器 `ExternalIdentityPO` 用于创建并初始化 `ExternalIdentityPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ExternalIdentityPO` creates and initializes `ExternalIdentityPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ExternalIdentityPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ExternalIdentityPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerCode 输入参数 `providerCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param externalSubjectId 输入参数 `externalSubjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ExternalIdentityPO(
            Long id,
            Long tenantId,
            String providerCode,
            String externalSubjectId,
            Long userId,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.providerCode = required(providerCode, "providerCode");
        this.externalSubjectId = required(externalSubjectId, "externalSubjectId");
        this.identitySub = this.externalSubjectId;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = ExternalIdentityStatusEnum.ACTIVE;
        this.lastSyncedAt = Objects.requireNonNull(now, "now");
        markCreated(actorId, now);
    }

    /**
     * 方法 `idpMapping` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `idp Mapping` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `idpMapping` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `idp Mapping` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `idpMapping` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `idpMapping`, then continue the business flow using its result, exception, or side effect.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public static ExternalIdentityPO idpMapping(
            Long id,
            Long tenantId,
            String identitySub,
            Long userId,
            String actorId,
            Instant now) {
        return new ExternalIdentityPO(
                id, tenantId, "IDP", identitySub, userId, actorId, now);
    }

    /**
     * 方法 `getId` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getIdentitySub` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `get Identity Sub` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getIdentitySub` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `get Identity Sub` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getUserId` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `get User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getUserId` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `get User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getStatus` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `get ExternalIdentityStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `get ExternalIdentityStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ExternalIdentityStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getLastSyncedAt` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `get Last Synced At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getLastSyncedAt` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `get Last Synced At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getLastSyncedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getLastSyncedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    /**
     * 方法 `required` 按照 `ExternalIdentityPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ExternalIdentityPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
