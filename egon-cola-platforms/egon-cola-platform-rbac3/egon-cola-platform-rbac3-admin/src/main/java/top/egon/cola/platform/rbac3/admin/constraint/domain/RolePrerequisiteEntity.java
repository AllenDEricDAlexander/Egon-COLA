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
 * 类型 `RolePrerequisiteEntity` 位于当前包内，是类型，用于承载 `Role Prerequisite Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RolePrerequisiteEntity` is a type in its package and carries the responsibility, state, or contract for `Role Prerequisite Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RolePrerequisiteEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RolePrerequisiteEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_role_prerequisite")
public class RolePrerequisiteEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `RolePrerequisiteEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `targetRoleId` 表示 `RolePrerequisiteEntity` 中与 `target Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `targetRoleId` stores the `target Role Id`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `targetRoleId` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `targetRoleId`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "target_role_id", nullable = false)
    private Long targetRoleId;
    /**
     * 字段 `groupCode` 表示 `RolePrerequisiteEntity` 中与 `group Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `groupCode` stores the `group Code`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `groupCode` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `groupCode`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "group_code", nullable = false, length = 128)
    private String groupCode;
    /**
     * 字段 `matchMode` 表示 `RolePrerequisiteEntity` 中与 `match Mode` 相关的状态、依赖、配置或结果（声明类型 `MatchMode`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `matchMode` stores the `match Mode`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `MatchMode`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `matchMode` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `matchMode`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false, length = 32)
    private MatchMode matchMode;
    /**
     * 字段 `prerequisiteRoleId` 表示 `RolePrerequisiteEntity` 中与 `prerequisite Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `prerequisiteRoleId` stores the `prerequisite Role Id`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `prerequisiteRoleId` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `prerequisiteRoleId`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "prerequisite_role_id", nullable = false)
    private Long prerequisiteRoleId;
    /**
     * 字段 `status` 表示 `RolePrerequisiteEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `RolePrerequisiteEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `RolePrerequisiteEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `RolePrerequisiteEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /**
     * 构造器 `RolePrerequisiteEntity` 用于创建并初始化 `RolePrerequisiteEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RolePrerequisiteEntity` creates and initializes `RolePrerequisiteEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RolePrerequisiteEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RolePrerequisiteEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RolePrerequisiteEntity() {
    }

    /**
     * 构造器 `RolePrerequisiteEntity` 用于创建并初始化 `RolePrerequisiteEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RolePrerequisiteEntity` creates and initializes `RolePrerequisiteEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RolePrerequisiteEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RolePrerequisiteEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param targetRoleId 输入参数 `targetRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param groupCode 输入参数 `groupCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param matchMode 输入参数 `matchMode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param prerequisiteRoleId 输入参数 `prerequisiteRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RolePrerequisiteEntity(
            Long id,
            Long tenantId,
            Long targetRoleId,
            String groupCode,
            MatchMode matchMode,
            Long prerequisiteRoleId,
            String actorId,
            Instant now) {
        if (targetRoleId.equals(prerequisiteRoleId)) {
            throw new IllegalArgumentException("prerequisite role must differ from target role");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.targetRoleId = Objects.requireNonNull(targetRoleId, "targetRoleId");
        this.groupCode = required(groupCode, "groupCode");
        this.matchMode = Objects.requireNonNull(matchMode, "matchMode");
        this.prerequisiteRoleId = Objects.requireNonNull(
                prerequisiteRoleId, "prerequisiteRoleId");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    /**
     * 类型 `MatchMode` 位于 `RolePrerequisiteEntity` 内，是枚举，用于承载 `Match Mode` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MatchMode` is an enum inside `RolePrerequisiteEntity` and carries the responsibility, state, or contract for `Match Mode`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MatchMode` 作为 `RolePrerequisiteEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MatchMode` as the responsibility boundary of `RolePrerequisiteEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum MatchMode {
        /**
         * 字段 `ALL_OF` 表示 `MatchMode` 中与 `ALL OF` 相关的状态、依赖、配置或结果（声明类型 `MatchMode`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ALL_OF` stores the `ALL OF`-related state, dependency, configuration, or result of `MatchMode` (declared type `MatchMode`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ALL_OF` 时应保持 `MatchMode` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ALL_OF`, preserve `MatchMode`'s lifecycle, immutability, and thread-safety constraints.
         */
        ALL_OF,
        /**
         * 字段 `ANY_OF` 表示 `MatchMode` 中与 `ANY OF` 相关的状态、依赖、配置或结果（声明类型 `MatchMode`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ANY_OF` stores the `ANY OF`-related state, dependency, configuration, or result of `MatchMode` (declared type `MatchMode`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ANY_OF` 时应保持 `MatchMode` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ANY_OF`, preserve `MatchMode`'s lifecycle, immutability, and thread-safety constraints.
         */
        ANY_OF
    }

    /**
     * 类型 `Status` 位于 `RolePrerequisiteEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `RolePrerequisiteEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `RolePrerequisiteEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `RolePrerequisiteEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
        DISABLED
    }

    /**
     * 方法 `required` 按照 `RolePrerequisiteEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `RolePrerequisiteEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
