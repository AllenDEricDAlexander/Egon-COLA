package top.egon.cola.platform.rbac3.admin.activation.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.activation.domain.SessionActiveRoleKey;

/**
 * 类型 `SessionActiveRolePO` 位于当前包内，是类型，用于承载 `Session Active Role Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SessionActiveRolePO` is a type in its package and carries the responsibility, state, or contract for `Session Active Role Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SessionActiveRolePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SessionActiveRolePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "SessionActiveRoleEntity")
@IdClass(SessionActiveRoleKey.class)
@Table(name = "rbac3_session_active_role")
public class SessionActiveRolePO {

    /**
     * 字段 `tenantId` 表示 `SessionActiveRolePO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    /**
     * 字段 `sessionId` 表示 `SessionActiveRolePO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "session_id")
    private Long sessionId;
    /**
     * 字段 `rootRoleId` 表示 `SessionActiveRolePO` 中与 `root Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `rootRoleId` stores the `root Role Id`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `rootRoleId` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `rootRoleId`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "root_role_id")
    private Long rootRoleId;
    /**
     * 字段 `applicationId` 表示 `SessionActiveRolePO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /**
     * 字段 `sessionVersion` 表示 `SessionActiveRolePO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "session_version", nullable = false)
    private long sessionVersion;
    /**
     * 字段 `eligibleAssignmentIds` 表示 `SessionActiveRolePO` 中与 `eligible Assignment Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `eligibleAssignmentIds` stores the `eligible Assignment Ids`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `eligibleAssignmentIds` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `eligibleAssignmentIds`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligible_assignment_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> eligibleAssignmentIds;
    /**
     * 字段 `activatedAt` 表示 `SessionActiveRolePO` 中与 `activated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `activatedAt` stores the `activated At`-related state, dependency, configuration, or result of `SessionActiveRolePO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `activatedAt` 时应保持 `SessionActiveRolePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `activatedAt`, preserve `SessionActiveRolePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    /**
     * 构造器 `SessionActiveRolePO` 用于创建并初始化 `SessionActiveRolePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionActiveRolePO` creates and initializes `SessionActiveRolePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionActiveRolePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionActiveRolePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected SessionActiveRolePO() {
    }

    /**
     * 构造器 `SessionActiveRolePO` 用于创建并初始化 `SessionActiveRolePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SessionActiveRolePO` creates and initializes `SessionActiveRolePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SessionActiveRolePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SessionActiveRolePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rootRoleId 输入参数 `rootRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eligibleAssignmentIds 输入参数 `eligibleAssignmentIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activatedAt 输入参数 `activatedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SessionActiveRolePO(
            Long tenantId,
            Long sessionId,
            Long applicationId,
            Long rootRoleId,
            long sessionVersion,
            List<String> eligibleAssignmentIds,
            Instant activatedAt
    ) {
        if (sessionVersion < 0) {
            throw new IllegalArgumentException("sessionVersion must not be negative");
        }
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.rootRoleId = Objects.requireNonNull(rootRoleId, "rootRoleId");
        this.sessionVersion = sessionVersion;
        this.eligibleAssignmentIds = List.copyOf(eligibleAssignmentIds);
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt");
    }

    /**
     * 方法 `getTenantId` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getTenantId` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getSessionId` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Session Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionId` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Session Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getApplicationId` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Application Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getApplicationId` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Application Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getApplicationId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getApplicationId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * 方法 `getRootRoleId` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Root Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRootRoleId` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Root Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRootRoleId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRootRoleId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getRootRoleId() {
        return rootRoleId;
    }

    /**
     * 方法 `getSessionVersion` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Session Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSessionVersion` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Session Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getEligibleAssignmentIds` 按照 `SessionActiveRolePO` 的职责处理输入，完成 `get Eligible Assignment Ids` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getEligibleAssignmentIds` processes its inputs according to `SessionActiveRolePO`'s responsibility, performs the `get Eligible Assignment Ids` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getEligibleAssignmentIds` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getEligibleAssignmentIds`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<String> getEligibleAssignmentIds() {
        return List.copyOf(eligibleAssignmentIds);
    }

    }
