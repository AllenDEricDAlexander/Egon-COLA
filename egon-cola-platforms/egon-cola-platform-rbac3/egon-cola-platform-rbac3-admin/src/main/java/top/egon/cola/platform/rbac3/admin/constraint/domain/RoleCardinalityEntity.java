package top.egon.cola.platform.rbac3.admin.constraint.domain;

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
 * 类型 `RoleCardinalityEntity` 位于当前包内，是类型，用于承载 `Role Cardinality Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleCardinalityEntity` is a type in its package and carries the responsibility, state, or contract for `Role Cardinality Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleCardinalityEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleCardinalityEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_role_cardinality")
public class RoleCardinalityEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `RoleCardinalityEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `roleId` 表示 `RoleCardinalityEntity` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    /**
     * 字段 `scopeType` 表示 `RoleCardinalityEntity` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    /**
     * 字段 `maximumActive` 表示 `RoleCardinalityEntity` 中与 `maximum Active` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `maximumActive` stores the `maximum Active`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `maximumActive` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `maximumActive`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "max_active", nullable = false)
    private int maximumActive;
    /**
     * 字段 `status` 表示 `RoleCardinalityEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `validFrom` 表示 `RoleCardinalityEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `RoleCardinalityEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `RoleCardinalityEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `RoleCardinalityEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `RoleCardinalityEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `RoleCardinalityEntity` 用于创建并初始化 `RoleCardinalityEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleCardinalityEntity` creates and initializes `RoleCardinalityEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleCardinalityEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleCardinalityEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RoleCardinalityEntity() {
    }

    /**
     * 构造器 `RoleCardinalityEntity` 用于创建并初始化 `RoleCardinalityEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleCardinalityEntity` creates and initializes `RoleCardinalityEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleCardinalityEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleCardinalityEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumActive 输入参数 `maximumActive`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleCardinalityEntity(
            Long id,
            Long tenantId,
            Long roleId,
            ScopeType scopeType,
            int maximumActive,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (maximumActive < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid role cardinality");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.maximumActive = maximumActive;
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    /**
     * 方法 `update` 按照 `RoleCardinalityEntity` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `RoleCardinalityEntity`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param maximumActive 输入参数 `maximumActive`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void update(
            ScopeType scopeType,
            int maximumActive,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (maximumActive < 1 || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid role cardinality");
        }
        this.scopeType = scopeType;
        this.maximumActive = maximumActive;
        this.validFrom = validFrom;
        this.validTo = validTo;
        markUpdated(actorId, now);
    }

    /**
     * 类型 `ScopeType` 位于 `RoleCardinalityEntity` 内，是枚举，用于承载 `Scope Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ScopeType` is an enum inside `RoleCardinalityEntity` and carries the responsibility, state, or contract for `Scope Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ScopeType` 作为 `RoleCardinalityEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ScopeType` as the responsibility boundary of `RoleCardinalityEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ScopeType {
        /**
         * 字段 `TENANT` 表示 `ScopeType` 中与 `TENANT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `TENANT` stores the `TENANT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `TENANT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `TENANT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        TENANT,
        /**
         * 字段 `ORG` 表示 `ScopeType` 中与 `ORG` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ORG` stores the `ORG`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ORG` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ORG`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ORG,
        /**
         * 字段 `DEPT` 表示 `ScopeType` 中与 `DEPT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPT` stores the `DEPT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPT
    }

    /**
     * 类型 `Status` 位于 `RoleCardinalityEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `RoleCardinalityEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `RoleCardinalityEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `RoleCardinalityEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
        EXPIRED
    }
}
