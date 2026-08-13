package top.egon.cola.platform.rbac3.admin.participation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `BusinessParticipationEntity` 位于当前包内，是类型，用于承载 `Business Participation Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `BusinessParticipationEntity` is a type in its package and carries the responsibility, state, or contract for `Business Participation Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Append-only evidence that a user performed an action on one business object.
 */
@Entity
@Table(name = "rbac3_business_participation")
public class BusinessParticipationEntity {

    /**
     * 字段 `id` 表示 `BusinessParticipationEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `tenantId` 表示 `BusinessParticipationEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `applicationCode` 表示 `BusinessParticipationEntity` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;
    /**
     * 字段 `businessResource` 表示 `BusinessParticipationEntity` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "business_resource", nullable = false, length = 128)
    private String businessResource;
    /**
     * 字段 `businessId` 表示 `BusinessParticipationEntity` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `businessId` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `businessId`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "business_id", nullable = false, length = 256)
    private String businessId;
    /**
     * 字段 `actorUserId` 表示 `BusinessParticipationEntity` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    /**
     * 字段 `actionCode` 表示 `BusinessParticipationEntity` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "action_code", nullable = false, length = 128)
    private String actionCode;
    /**
     * 字段 `businessEventId` 表示 `BusinessParticipationEntity` 中与 `business Event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `businessEventId` stores the `business Event Id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `businessEventId` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `businessEventId`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "business_event_id", nullable = false, length = 128)
    private String businessEventId;
    /**
     * 字段 `occurredAt` 表示 `BusinessParticipationEntity` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    /**
     * 字段 `traceId` 表示 `BusinessParticipationEntity` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `traceId` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `traceId`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;
    /**
     * 字段 `payloadDigest` 表示 `BusinessParticipationEntity` 中与 `payload Digest` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `payloadDigest` stores the `payload Digest`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `payloadDigest` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `payloadDigest`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "payload_digest", nullable = false, length = 128)
    private String payloadDigest;
    /**
     * 字段 `createdAt` 表示 `BusinessParticipationEntity` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    /**
     * 字段 `createdBy` 表示 `BusinessParticipationEntity` 中与 `created By` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdBy` stores the `created By`-related state, dependency, configuration, or result of `BusinessParticipationEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdBy` 时应保持 `BusinessParticipationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdBy`, preserve `BusinessParticipationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    /**
     * 构造器 `BusinessParticipationEntity` 用于创建并初始化 `BusinessParticipationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `BusinessParticipationEntity` creates and initializes `BusinessParticipationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `BusinessParticipationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `BusinessParticipationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected BusinessParticipationEntity() {
    }

    /**
     * 构造器 `BusinessParticipationEntity` 用于创建并初始化 `BusinessParticipationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `BusinessParticipationEntity` creates and initializes `BusinessParticipationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `BusinessParticipationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `BusinessParticipationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessId 输入参数 `businessId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorUserId 输入参数 `actorUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actionCode 输入参数 `actionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessEventId 输入参数 `businessEventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param payloadDigest 输入参数 `payloadDigest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param createdAt 输入参数 `createdAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param createdBy 输入参数 `createdBy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public BusinessParticipationEntity(
            Long id,
            Long tenantId,
            String applicationCode,
            String businessResource,
            String businessId,
            Long actorUserId,
            String actionCode,
            String businessEventId,
            Instant occurredAt,
            String traceId,
            String payloadDigest,
            Instant createdAt,
            String createdBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.applicationCode = required(applicationCode, "applicationCode");
        this.businessResource = required(businessResource, "businessResource");
        this.businessId = required(businessId, "businessId");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId");
        this.actionCode = required(actionCode, "actionCode");
        this.businessEventId = required(businessEventId, "businessEventId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.traceId = required(traceId, "traceId");
        this.payloadDigest = required(payloadDigest, "payloadDigest");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.createdBy = required(createdBy, "createdBy");
    }

    /**
     * 方法 `getId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getTenantId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTenantId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 方法 `getApplicationCode` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Application Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationCode` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Application Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getApplicationCode() {
        return applicationCode;
    }

    /**
     * 方法 `getBusinessResource` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Business Resource` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBusinessResource` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Business Resource` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBusinessResource` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBusinessResource`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getBusinessResource() {
        return businessResource;
    }

    /**
     * 方法 `getBusinessId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Business Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBusinessId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Business Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBusinessId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBusinessId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getBusinessId() {
        return businessId;
    }

    /**
     * 方法 `getActorUserId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Actor User Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActorUserId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Actor User Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActorUserId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActorUserId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getActorUserId() {
        return actorUserId;
    }

    /**
     * 方法 `getActionCode` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Action Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActionCode` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Action Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActionCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActionCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getActionCode() {
        return actionCode;
    }

    /**
     * 方法 `getBusinessEventId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Business Event Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBusinessEventId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Business Event Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBusinessEventId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBusinessEventId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getBusinessEventId() {
        return businessEventId;
    }

    /**
     * 方法 `getOccurredAt` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Occurred At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getOccurredAt` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Occurred At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getOccurredAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getOccurredAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * 方法 `getTraceId` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Trace Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTraceId` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Trace Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTraceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTraceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 方法 `getPayloadDigest` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Payload Digest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPayloadDigest` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Payload Digest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPayloadDigest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPayloadDigest`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPayloadDigest() {
        return payloadDigest;
    }

    /**
     * 方法 `getCreatedAt` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `get Created At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCreatedAt` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `get Created At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCreatedAt` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCreatedAt`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 方法 `required` 按照 `BusinessParticipationEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `BusinessParticipationEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
