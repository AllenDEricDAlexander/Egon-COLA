package top.egon.cola.platform.rbac3.admin.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `AuditLogEntity` 位于当前包内，是类型，用于承载 `Audit Log Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditLogEntity` is a type in its package and carries the responsibility, state, or contract for `Audit Log Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Append-only, already-redacted security and authorization audit record.
 */
@Entity
@Table(name = "rbac3_audit_log")
public class AuditLogEntity {

    /**
     * 字段 `id` 表示 `AuditLogEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `tenantId` 表示 `AuditLogEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `eventType` 表示 `AuditLogEntity` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;
    /**
     * 字段 `outcome` 表示 `AuditLogEntity` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 32)
    private String outcome;
    /**
     * 字段 `severity` 表示 `AuditLogEntity` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 32)
    private String severity;
    /**
     * 字段 `actorType` 表示 `AuditLogEntity` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actorType` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actorType`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "actor_type", nullable = false, length = 32)
    private String actorType;
    /**
     * 字段 `actorId` 表示 `AuditLogEntity` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;
    /**
     * 字段 `targetType` 表示 `AuditLogEntity` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "target_type", length = 128)
    private String targetType;
    /**
     * 字段 `targetId` 表示 `AuditLogEntity` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "target_id", length = 128)
    private String targetId;
    /**
     * 字段 `managementPolicyId` 表示 `AuditLogEntity` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "management_policy_id")
    private Long managementPolicyId;
    /**
     * 字段 `reasonCode` 表示 `AuditLogEntity` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "reason_code", length = 128)
    private String reasonCode;
    /**
     * 字段 `requestId` 表示 `AuditLogEntity` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;
    /**
     * 字段 `traceId` 表示 `AuditLogEntity` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;
    /**
     * 字段 `clientIp` 表示 `AuditLogEntity` 中与 `client Ip` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clientIp` stores the `client Ip`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clientIp` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clientIp`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "client_ip", columnDefinition = "inet")
    private String clientIp;
    /**
     * 字段 `userAgent` 表示 `AuditLogEntity` 中与 `user Agent` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `userAgent` stores the `user Agent`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `userAgent` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `userAgent`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
    /**
     * 字段 `beforeSnapshot` 表示 `AuditLogEntity` 中与 `before Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `beforeSnapshot` stores the `before Snapshot`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `beforeSnapshot` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `beforeSnapshot`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> beforeSnapshot;
    /**
     * 字段 `afterSnapshot` 表示 `AuditLogEntity` 中与 `after Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `afterSnapshot` stores the `after Snapshot`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `afterSnapshot` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `afterSnapshot`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> afterSnapshot;
    /**
     * 字段 `payloadChecksum` 表示 `AuditLogEntity` 中与 `payload Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `payloadChecksum` stores the `payload Checksum`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `payloadChecksum` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `payloadChecksum`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "payload_checksum", nullable = false, length = 256)
    private String payloadChecksum;
    /**
     * 字段 `createdAt` 表示 `AuditLogEntity` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `AuditLogEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `AuditLogEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `AuditLogEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 构造器 `AuditLogEntity` 用于创建并初始化 `AuditLogEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuditLogEntity` creates and initializes `AuditLogEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuditLogEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuditLogEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected AuditLogEntity() {
    }

    /**
     * 构造器 `AuditLogEntity` 用于创建并初始化 `AuditLogEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuditLogEntity` creates and initializes `AuditLogEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuditLogEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuditLogEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param severity 输入参数 `severity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorType 输入参数 `actorType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param managementPolicyId 输入参数 `managementPolicyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clientIp 输入参数 `clientIp`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userAgent 输入参数 `userAgent`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param beforeSnapshot 输入参数 `beforeSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param afterSnapshot 输入参数 `afterSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param payloadChecksum 输入参数 `payloadChecksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param createdAt 输入参数 `createdAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuditLogEntity(
            Long id,
            Long tenantId,
            String eventType,
            String outcome,
            String severity,
            String actorType,
            String actorId,
            String targetType,
            String targetId,
            Long managementPolicyId,
            String reasonCode,
            String requestId,
            String traceId,
            String clientIp,
            String userAgent,
            Map<String, Object> beforeSnapshot,
            Map<String, Object> afterSnapshot,
            String payloadChecksum,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.eventType = required(eventType, "eventType");
        this.outcome = required(outcome, "outcome");
        this.severity = required(severity, "severity");
        this.actorType = required(actorType, "actorType");
        this.actorId = required(actorId, "actorId");
        this.targetType = optional(targetType, "targetType");
        this.targetId = optional(targetId, "targetId");
        this.managementPolicyId = managementPolicyId;
        this.reasonCode = optional(reasonCode, "reasonCode");
        this.requestId = required(requestId, "requestId");
        this.traceId = required(traceId, "traceId");
        this.clientIp = optional(clientIp, "clientIp");
        this.userAgent = optional(userAgent, "userAgent");
        this.beforeSnapshot = Map.copyOf(Objects.requireNonNull(
                beforeSnapshot, "beforeSnapshot"));
        this.afterSnapshot = Map.copyOf(Objects.requireNonNull(
                afterSnapshot, "afterSnapshot"));
        this.payloadChecksum = required(payloadChecksum, "payloadChecksum");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * 方法 `getId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getTenantId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTenantId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getEventType` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Event Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getEventType` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Event Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getEventType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getEventType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 方法 `getOutcome` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Outcome` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getOutcome` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Outcome` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getOutcome` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getOutcome`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getOutcome() {
        return outcome;
    }

    /**
     * 方法 `getSeverity` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Severity` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSeverity` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Severity` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSeverity` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSeverity`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * 方法 `getActorType` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Actor Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActorType` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Actor Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActorType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActorType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getActorType() {
        return actorType;
    }

    /**
     * 方法 `getActorId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Actor Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getActorId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Actor Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getActorId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getActorId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getActorId() {
        return actorId;
    }

    /**
     * 方法 `getTargetType` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Target Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTargetType` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Target Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTargetType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTargetType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 方法 `getTargetId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Target Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTargetId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Target Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getTargetId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getTargetId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * 方法 `getManagementPolicyId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Management Policy Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getManagementPolicyId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Management Policy Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getManagementPolicyId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getManagementPolicyId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getManagementPolicyId() {
        return managementPolicyId;
    }

    /**
     * 方法 `getReasonCode` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Reason Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReasonCode` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Reason Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReasonCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReasonCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getReasonCode() {
        return reasonCode;
    }

    /**
     * 方法 `getRequestId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Request Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRequestId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Request Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRequestId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRequestId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 方法 `getTraceId` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Trace Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTraceId` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Trace Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getBeforeSnapshot` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Before Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getBeforeSnapshot` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Before Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getBeforeSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getBeforeSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getBeforeSnapshot() {
        return beforeSnapshot;
    }

    /**
     * 方法 `getAfterSnapshot` 按照 `AuditLogEntity` 的职责处理输入，完成 `get After Snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getAfterSnapshot` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get After Snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getAfterSnapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getAfterSnapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getAfterSnapshot() {
        return afterSnapshot;
    }

    /**
     * 方法 `getPayloadChecksum` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Payload Checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPayloadChecksum` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Payload Checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPayloadChecksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPayloadChecksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getPayloadChecksum() {
        return payloadChecksum;
    }

    /**
     * 方法 `getCreatedAt` 按照 `AuditLogEntity` 的职责处理输入，完成 `get Created At` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCreatedAt` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `get Created At` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `required` 按照 `AuditLogEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `optional` 按照 `AuditLogEntity` 的职责处理输入，完成 `optional` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `optional` processes its inputs according to `AuditLogEntity`'s responsibility, performs the `optional` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `optional` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `optional`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
