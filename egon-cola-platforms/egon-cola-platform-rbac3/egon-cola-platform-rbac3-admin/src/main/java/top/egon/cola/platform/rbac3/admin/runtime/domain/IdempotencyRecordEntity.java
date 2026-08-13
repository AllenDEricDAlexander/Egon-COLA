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
 * 类型 `IdempotencyRecordEntity` 位于当前包内，是类型，用于承载 `Idempotency Record Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `IdempotencyRecordEntity` is a type in its package and carries the responsibility, state, or contract for `Idempotency Record Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `IdempotencyRecordEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `IdempotencyRecordEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_idempotency_record")
public class IdempotencyRecordEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `IdempotencyRecordEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `actorType` 表示 `IdempotencyRecordEntity` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `ActorType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `ActorType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actorType` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actorType`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;
    /**
     * 字段 `actorId` 表示 `IdempotencyRecordEntity` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actorId` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actorId`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;
    /**
     * 字段 `operationCode` 表示 `IdempotencyRecordEntity` 中与 `operation Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `operationCode` stores the `operation Code`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `operationCode` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `operationCode`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "operation_code", nullable = false, length = 128)
    private String operationCode;
    /**
     * 字段 `keyHash` 表示 `IdempotencyRecordEntity` 中与 `key Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keyHash` stores the `key Hash`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keyHash` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keyHash`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;
    /**
     * 字段 `requestHash` 表示 `IdempotencyRecordEntity` 中与 `request Hash` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requestHash` stores the `request Hash`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requestHash` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requestHash`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "request_hash", nullable = false, length = 256)
    private String requestHash;
    /**
     * 字段 `resourceType` 表示 `IdempotencyRecordEntity` 中与 `resource Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceType` stores the `resource Type`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceType` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceType`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_type", length = 128)
    private String resourceType;
    /**
     * 字段 `resourceId` 表示 `IdempotencyRecordEntity` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "resource_id", length = 128)
    private String resourceId;
    /**
     * 字段 `responseStatus` 表示 `IdempotencyRecordEntity` 中与 `response Status` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `responseStatus` stores the `response Status`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `responseStatus` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `responseStatus`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "response_status")
    private Integer responseStatus;
    /**
     * 字段 `responseDigest` 表示 `IdempotencyRecordEntity` 中与 `response Digest` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `responseDigest` stores the `response Digest`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `responseDigest` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `responseDigest`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "response_digest", length = 256)
    private String responseDigest;
    /**
     * 字段 `status` 表示 `IdempotencyRecordEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `expiresAt` 表示 `IdempotencyRecordEntity` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `IdempotencyRecordEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `IdempotencyRecordEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `IdempotencyRecordEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 构造器 `IdempotencyRecordEntity` 用于创建并初始化 `IdempotencyRecordEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdempotencyRecordEntity` creates and initializes `IdempotencyRecordEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdempotencyRecordEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdempotencyRecordEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected IdempotencyRecordEntity() {
    }

    /**
     * 构造器 `IdempotencyRecordEntity` 用于创建并初始化 `IdempotencyRecordEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `IdempotencyRecordEntity` creates and initializes `IdempotencyRecordEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `IdempotencyRecordEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `IdempotencyRecordEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorType 输入参数 `actorType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operationCode 输入参数 `operationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyHash 输入参数 `keyHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestHash 输入参数 `requestHash`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expiresAt 输入参数 `expiresAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public IdempotencyRecordEntity(
            Long id,
            Long tenantId,
            ActorType actorType,
            String actorId,
            String operationCode,
            String keyHash,
            String requestHash,
            Instant expiresAt,
            Instant now
    ) {
        this.id = id;
        setTenantId(tenantId);
        this.actorType = actorType;
        this.actorId = required(actorId);
        this.operationCode = required(operationCode);
        this.keyHash = required(keyHash);
        this.requestHash = required(requestHash);
        this.status = Status.PROCESSING;
        this.expiresAt = expiresAt;
        markCreated(actorId, now);
    }

    /**
     * 方法 `complete` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resourceType 输入参数 `resourceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param responseStatus 输入参数 `responseStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param responseDigest 输入参数 `responseDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void complete(
            String resourceType,
            String resourceId,
            int responseStatus,
            String responseDigest,
            Instant now
    ) {
        if (responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException("invalid response status");
        }
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.responseStatus = responseStatus;
        this.responseDigest = responseDigest;
        this.status = Status.COMPLETED;
        markUpdated(actorId, now);
    }

    /**
     * 方法 `getRequestHash` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `get Request Hash` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRequestHash` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `get Request Hash` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRequestHash` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRequestHash`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getRequestHash() {
        return requestHash;
    }

    /**
     * 方法 `getResourceId` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `get Resource Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResourceId` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `get Resource Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResourceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResourceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * 方法 `getResponseStatus` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `get Response Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResponseStatus` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `get Response Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResponseStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResponseStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Integer getResponseStatus() {
        return responseStatus;
    }

    /**
     * 方法 `getResponseDigest` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `get Response Digest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getResponseDigest` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `get Response Digest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getResponseDigest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getResponseDigest`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getResponseDigest() {
        return responseDigest;
    }

    /**
     * 方法 `getStatus` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `get Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `get Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `IdempotencyRecordEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `IdempotencyRecordEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value is required");
        }
        return value.trim();
    }

    /**
     * 类型 `ActorType` 位于 `IdempotencyRecordEntity` 内，是枚举，用于承载 `Actor Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActorType` is an enum inside `IdempotencyRecordEntity` and carries the responsibility, state, or contract for `Actor Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActorType` 作为 `IdempotencyRecordEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActorType` as the responsibility boundary of `IdempotencyRecordEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ActorType {
        /**
         * 字段 `USER` 表示 `ActorType` 中与 `USER` 相关的状态、依赖、配置或结果（声明类型 `ActorType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `USER` stores the `USER`-related state, dependency, configuration, or result of `ActorType` (declared type `ActorType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `USER` 时应保持 `ActorType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `USER`, preserve `ActorType`'s lifecycle, immutability, and thread-safety constraints.
         */
        USER,
        /**
         * 字段 `SERVICE` 表示 `ActorType` 中与 `SERVICE` 相关的状态、依赖、配置或结果（声明类型 `ActorType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SERVICE` stores the `SERVICE`-related state, dependency, configuration, or result of `ActorType` (declared type `ActorType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SERVICE` 时应保持 `ActorType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SERVICE`, preserve `ActorType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SERVICE,
        /**
         * 字段 `SYSTEM` 表示 `ActorType` 中与 `SYSTEM` 相关的状态、依赖、配置或结果（声明类型 `ActorType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SYSTEM` stores the `SYSTEM`-related state, dependency, configuration, or result of `ActorType` (declared type `ActorType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SYSTEM` 时应保持 `ActorType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SYSTEM`, preserve `ActorType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SYSTEM
    }

    /**
     * 类型 `Status` 位于 `IdempotencyRecordEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `IdempotencyRecordEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `IdempotencyRecordEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `IdempotencyRecordEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Status {
        /**
         * 字段 `PROCESSING` 表示 `Status` 中与 `PROCESSING` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PROCESSING` stores the `PROCESSING`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PROCESSING` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PROCESSING`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        PROCESSING,
        /**
         * 字段 `COMPLETED` 表示 `Status` 中与 `COMPLETED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `COMPLETED` stores the `COMPLETED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `COMPLETED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `COMPLETED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        COMPLETED,
        /**
         * 字段 `FAILED` 表示 `Status` 中与 `FAILED` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `FAILED` stores the `FAILED`-related state, dependency, configuration, or result of `Status` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `FAILED` 时应保持 `Status` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FAILED`, preserve `Status`'s lifecycle, immutability, and thread-safety constraints.
         */
        FAILED
    }
}
