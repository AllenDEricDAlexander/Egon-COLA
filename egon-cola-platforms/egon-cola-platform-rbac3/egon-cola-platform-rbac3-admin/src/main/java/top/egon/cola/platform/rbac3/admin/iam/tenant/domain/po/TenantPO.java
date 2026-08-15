package top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.enums.TenantStatusEnum;

/**
 * 类型 `TenantPO` 位于当前包内，是类型，用于承载 `Tenant Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TenantPO` is a type in its package and carries the responsibility, state, or contract for `Tenant Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `TenantPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `TenantPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "TenantEntity")
@Table(name = "rbac3_tenant")
public class TenantPO {

    /**
     * 字段 `id` 表示 `TenantPO` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `TenantPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;

    /**
     * 字段 `code` 表示 `TenantPO` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `code` stores the `code`-related state, dependency, configuration, or result of `TenantPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `code` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `code`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 64)
    private String code;

    /**
     * 字段 `name` 表示 `TenantPO` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `name` stores the `name`-related state, dependency, configuration, or result of `TenantPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `name` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `name`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 字段 `status` 表示 `TenantPO` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `TenantStatusEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `TenantPO` (declared type `TenantStatusEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantStatusEnum status;

    /**
     * 字段 `policyVersion` 表示 `TenantPO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `TenantPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "policy_version", nullable = false)
    private long policyVersion;

    /**
     * 字段 `settings` 表示 `TenantPO` 中与 `settings` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `settings` stores the `settings`-related state, dependency, configuration, or result of `TenantPO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `settings` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `settings`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> settings = new LinkedHashMap<>();

    /**
     * 字段 `version` 表示 `TenantPO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `version` stores the `version`-related state, dependency, configuration, or result of `TenantPO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `version` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `version`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * 字段 `createdAt` 表示 `TenantPO` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `TenantPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 字段 `createdBy` 表示 `TenantPO` 中与 `created By` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `createdBy` stores the `created By`-related state, dependency, configuration, or result of `TenantPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `createdBy` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `createdBy`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    /**
     * 字段 `updatedAt` 表示 `TenantPO` 中与 `updated At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `updatedAt` stores the `updated At`-related state, dependency, configuration, or result of `TenantPO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `updatedAt` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `updatedAt`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 字段 `updatedBy` 表示 `TenantPO` 中与 `updated By` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `updatedBy` stores the `updated By`-related state, dependency, configuration, or result of `TenantPO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `updatedBy` 时应保持 `TenantPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `updatedBy`, preserve `TenantPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    /**
     * 构造器 `TenantPO` 用于创建并初始化 `TenantPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `TenantPO` creates and initializes `TenantPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `TenantPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `TenantPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected TenantPO() {
    }

    /**
     * 构造器 `TenantPO` 用于创建并初始化 `TenantPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `TenantPO` creates and initializes `TenantPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `TenantPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `TenantPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public TenantPO(Long id, String code, String name, String actorId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.status = TenantStatusEnum.INITIALIZING;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.createdBy = required(actorId, "actorId");
        this.updatedAt = now;
        this.updatedBy = this.createdBy;
    }

    /**
     * 方法 `configure` 按照 `TenantPO` 的职责处理输入，完成 `configure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `configure` processes its inputs according to `TenantPO`'s responsibility, performs the `configure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `configure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `configure`, then continue the business flow using its result, exception, or side effect.
     *
     * @param newSettings 输入参数 `newSettings`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void configure(Map<String, Object> newSettings, String actorId, Instant now) {
        settings = new LinkedHashMap<>(Objects.requireNonNull(newSettings, "newSettings"));
        touch(actorId, now);
    }

    /**
     * 方法 `activate` 按照 `TenantPO` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `TenantPO`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void activate(String actorId, Instant now) {
        if (status == TenantStatusEnum.CLOSED) {
            throw new IllegalStateException("closed tenant cannot be activated");
        }
        status = TenantStatusEnum.ACTIVE;
        touch(actorId, now);
    }

    /**
     * 方法 `changeStatus` 按照 `TenantPO` 的职责处理输入，完成 `change TenantStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeStatus` processes its inputs according to `TenantPO`'s responsibility, performs the `change TenantStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param nextStatus 输入参数 `nextStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean changeStatus(
            TenantStatusEnum nextStatus,
            long expectedVersion,
            String reason,
            String actorId,
            Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        required(reason, "reason");
        if (version != expectedVersion) {
            throw new IllegalStateException("tenant version conflict");
        }
        if (status == TenantStatusEnum.CLOSED && nextStatus != TenantStatusEnum.CLOSED) {
            throw new IllegalStateException("closed tenant is terminal");
        }
        if (status == nextStatus) {
            return false;
        }
        status = nextStatus;
        touch(actorId, now);
        return true;
    }

    /**
     * 方法 `incrementPolicyVersion` 按照 `TenantPO` 的职责处理输入，完成 `increment Policy Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `incrementPolicyVersion` processes its inputs according to `TenantPO`'s responsibility, performs the `increment Policy Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `incrementPolicyVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `incrementPolicyVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void incrementPolicyVersion(String actorId, Instant now) {
        policyVersion = Math.incrementExact(policyVersion);
        touch(actorId, now);
    }

    /**
     * 方法 `getId` 按照 `TenantPO` 的职责处理输入，完成 `get Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getId` processes its inputs according to `TenantPO`'s responsibility, performs the `get Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
     * 方法 `getCode` 按照 `TenantPO` 的职责处理输入，完成 `get Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getCode` processes its inputs according to `TenantPO`'s responsibility, performs the `get Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getCode`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getCode() {
        return code;
    }

    /**
     * 方法 `getName` 按照 `TenantPO` 的职责处理输入，完成 `get Name` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getName` processes its inputs according to `TenantPO`'s responsibility, performs the `get Name` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getName` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getName`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public String getName() {
        return name;
    }

    /**
     * 方法 `getStatus` 按照 `TenantPO` 的职责处理输入，完成 `get TenantStatusEnum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getStatus` processes its inputs according to `TenantPO`'s responsibility, performs the `get TenantStatusEnum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public TenantStatusEnum getStatus() {
        return status;
    }

    /**
     * 方法 `getPolicyVersion` 按照 `TenantPO` 的职责处理输入，完成 `get Policy Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getPolicyVersion` processes its inputs according to `TenantPO`'s responsibility, performs the `get Policy Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getPolicyVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getPolicyVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getPolicyVersion() {
        return policyVersion;
    }

    /**
     * 方法 `getSettings` 按照 `TenantPO` 的职责处理输入，完成 `get Settings` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSettings` processes its inputs according to `TenantPO`'s responsibility, performs the `get Settings` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSettings` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSettings`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Map<String, Object> getSettings() {
        return Map.copyOf(settings);
    }

    /**
     * 方法 `getVersion` 按照 `TenantPO` 的职责处理输入，完成 `get Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getVersion` processes its inputs according to `TenantPO`'s responsibility, performs the `get Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public long getVersion() {
        return version;
    }

    /**
     * 方法 `touch` 按照 `TenantPO` 的职责处理输入，完成 `touch` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `touch` processes its inputs according to `TenantPO`'s responsibility, performs the `touch` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `touch` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `touch`, then continue the business flow using its result, exception, or side effect.
     *
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void touch(String actorId, Instant now) {
        updatedBy = required(actorId, "actorId");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * 方法 `required` 按照 `TenantPO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `TenantPO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
