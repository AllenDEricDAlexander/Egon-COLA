package top.egon.cola.platform.rbac3.admin.auth.domain;

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
 * 类型 `ServiceCredentialEntity` 位于当前包内，是类型，用于承载 `Service Credential Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ServiceCredentialEntity` is a type in its package and carries the responsibility, state, or contract for `Service Credential Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ServiceCredentialEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ServiceCredentialEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_service_credential")
public class ServiceCredentialEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `ServiceCredentialEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `principalId` 表示 `ServiceCredentialEntity` 中与 `principal Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `principalId` stores the `principal Id`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `principalId` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `principalId`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    /**
     * 字段 `credentialId` 表示 `ServiceCredentialEntity` 中与 `credential Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialId` stores the `credential Id`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialId` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialId`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "credential_id", nullable = false, length = 128)
    private String credentialId;

    /**
     * 字段 `credentialType` 表示 `ServiceCredentialEntity` 中与 `credential Type` 相关的状态、依赖、配置或结果（声明类型 `CredentialType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialType` stores the `credential Type`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `CredentialType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialType` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialType`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 32)
    private CredentialType credentialType;

    /**
     * 字段 `secretHash` 表示 `ServiceCredentialEntity` 中与 `secret Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `secretHash` stores the `secret Hash`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `secretHash` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `secretHash`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "secret_hash", length = 512)
    private String secretHash;

    /**
     * 字段 `publicKey` 表示 `ServiceCredentialEntity` 中与 `public Key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `publicKey` stores the `public Key`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `publicKey` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `publicKey`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "public_key", columnDefinition = "text")
    private String publicKey;

    /**
     * 字段 `validFrom` 表示 `ServiceCredentialEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /**
     * 字段 `validTo` 表示 `ServiceCredentialEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 字段 `status` 表示 `ServiceCredentialEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 字段 `lastUsedAt` 表示 `ServiceCredentialEntity` 中与 `last Used At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lastUsedAt` stores the `last Used At`-related state, dependency, configuration, or result of `ServiceCredentialEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lastUsedAt` 时应保持 `ServiceCredentialEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lastUsedAt`, preserve `ServiceCredentialEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 构造器 `ServiceCredentialEntity` 用于创建并初始化 `ServiceCredentialEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ServiceCredentialEntity` creates and initializes `ServiceCredentialEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ServiceCredentialEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ServiceCredentialEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ServiceCredentialEntity() {
    }

    /**
     * 构造器 `ServiceCredentialEntity` 用于创建并初始化 `ServiceCredentialEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ServiceCredentialEntity` creates and initializes `ServiceCredentialEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ServiceCredentialEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ServiceCredentialEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principalId 输入参数 `principalId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentialId 输入参数 `credentialId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentialType 输入参数 `credentialType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param secretHash 输入参数 `secretHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param publicKey 输入参数 `publicKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ServiceCredentialEntity(
            Long id,
            Long tenantId,
            Long principalId,
            String credentialId,
            CredentialType credentialType,
            String secretHash,
            String publicKey,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        boolean validClientSecret = credentialType == CredentialType.CLIENT_SECRET
                && secretHash != null && publicKey == null;
        boolean validPublicKey = credentialType == CredentialType.PUBLIC_KEY
                && secretHash == null && publicKey != null;
        if (!validClientSecret && !validPublicKey) {
            throw new IllegalArgumentException("credential material does not match credential type");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.principalId = Objects.requireNonNull(principalId, "principalId");
        this.credentialId = required(credentialId, "credentialId");
        this.credentialType = Objects.requireNonNull(credentialType, "credentialType");
        this.secretHash = secretHash;
        this.publicKey = publicKey;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 方法 `required` 按照 `ServiceCredentialEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ServiceCredentialEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 类型 `CredentialType` 位于 `ServiceCredentialEntity` 内，是枚举，用于承载 `Credential Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CredentialType` is an enum inside `ServiceCredentialEntity` and carries the responsibility, state, or contract for `Credential Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CredentialType` 作为 `ServiceCredentialEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CredentialType` as the responsibility boundary of `ServiceCredentialEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum CredentialType {
        /**
         * 字段 `CLIENT_SECRET` 表示 `CredentialType` 中与 `CLIENT SECRET` 相关的状态、依赖、配置或结果（声明类型 `CredentialType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CLIENT_SECRET` stores the `CLIENT SECRET`-related state, dependency, configuration, or result of `CredentialType` (declared type `CredentialType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CLIENT_SECRET` 时应保持 `CredentialType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CLIENT_SECRET`, preserve `CredentialType`'s lifecycle, immutability, and thread-safety constraints.
         */
        CLIENT_SECRET,
        /**
         * 字段 `PUBLIC_KEY` 表示 `CredentialType` 中与 `PUBLIC KEY` 相关的状态、依赖、配置或结果（声明类型 `CredentialType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PUBLIC_KEY` stores the `PUBLIC KEY`-related state, dependency, configuration, or result of `CredentialType` (declared type `CredentialType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PUBLIC_KEY` 时应保持 `CredentialType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PUBLIC_KEY`, preserve `CredentialType`'s lifecycle, immutability, and thread-safety constraints.
         */
        PUBLIC_KEY
    }

    /**
     * 类型 `Status` 位于 `ServiceCredentialEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `ServiceCredentialEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `ServiceCredentialEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `ServiceCredentialEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
