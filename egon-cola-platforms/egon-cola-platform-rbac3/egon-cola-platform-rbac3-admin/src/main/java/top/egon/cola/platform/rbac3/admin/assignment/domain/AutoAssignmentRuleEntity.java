package top.egon.cola.platform.rbac3.admin.assignment.domain;

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
 * 类型 `AutoAssignmentRuleEntity` 位于当前包内，是类型，用于承载 `Auto Assignment Rule Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AutoAssignmentRuleEntity` is a type in its package and carries the responsibility, state, or contract for `Auto Assignment Rule Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AutoAssignmentRuleEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AutoAssignmentRuleEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_auto_assignment_rule")
public class AutoAssignmentRuleEntity extends TenantScopedEntity {

    /**
     * 字段 `id` 表示 `AutoAssignmentRuleEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `ruleCode` 表示 `AutoAssignmentRuleEntity` 中与 `rule Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ruleCode` stores the `rule Code`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ruleCode` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ruleCode`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "rule_code", nullable = false, length = 128)
    private String ruleCode;
    /**
     * 字段 `matchType` 表示 `AutoAssignmentRuleEntity` 中与 `match Type` 相关的状态、依赖、配置或结果（声明类型 `MatchType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `matchType` stores the `match Type`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `MatchType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `matchType` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `matchType`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 32)
    private MatchType matchType;
    /**
     * 字段 `matchReferenceId` 表示 `AutoAssignmentRuleEntity` 中与 `match Reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `matchReferenceId` stores the `match Reference Id`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `matchReferenceId` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `matchReferenceId`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "match_ref_id")
    private Long matchReferenceId;
    /**
     * 字段 `roleId` 表示 `AutoAssignmentRuleEntity` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    /**
     * 字段 `status` 表示 `AutoAssignmentRuleEntity` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `Status`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `status` stores the `status`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Status`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `status` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `status`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    /**
     * 字段 `validFrom` 表示 `AutoAssignmentRuleEntity` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    /**
     * 字段 `validTo` 表示 `AutoAssignmentRuleEntity` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AutoAssignmentRuleEntity` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AutoAssignmentRuleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AutoAssignmentRuleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "valid_to")
    private Instant validTo;

    /**
     * 构造器 `AutoAssignmentRuleEntity` 用于创建并初始化 `AutoAssignmentRuleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AutoAssignmentRuleEntity` creates and initializes `AutoAssignmentRuleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AutoAssignmentRuleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AutoAssignmentRuleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected AutoAssignmentRuleEntity() {
    }

    /**
     * 构造器 `AutoAssignmentRuleEntity` 用于创建并初始化 `AutoAssignmentRuleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AutoAssignmentRuleEntity` creates and initializes `AutoAssignmentRuleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AutoAssignmentRuleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AutoAssignmentRuleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ruleCode 输入参数 `ruleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param matchType 输入参数 `matchType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param matchReferenceId 输入参数 `matchReferenceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AutoAssignmentRuleEntity(
            Long id,
            Long tenantId,
            String ruleCode,
            MatchType matchType,
            Long matchReferenceId,
            Long roleId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now
    ) {
        if (matchType == MatchType.ALL_ACTIVE_USERS && matchReferenceId != null
                || matchType == MatchType.POSITION && matchReferenceId == null
                || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid auto-assignment rule");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.ruleCode = required(ruleCode);
        this.matchType = Objects.requireNonNull(matchType, "matchType");
        this.matchReferenceId = matchReferenceId;
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    /**
     * 方法 `required` 按照 `AutoAssignmentRuleEntity` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AutoAssignmentRuleEntity`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ruleCode is required");
        }
        return value.trim();
    }

    /**
     * 类型 `MatchType` 位于 `AutoAssignmentRuleEntity` 内，是枚举，用于承载 `Match Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MatchType` is an enum inside `AutoAssignmentRuleEntity` and carries the responsibility, state, or contract for `Match Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MatchType` 作为 `AutoAssignmentRuleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MatchType` as the responsibility boundary of `AutoAssignmentRuleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum MatchType {
        /**
         * 字段 `ALL_ACTIVE_USERS` 表示 `MatchType` 中与 `ALL ACTIVE USERS` 相关的状态、依赖、配置或结果（声明类型 `MatchType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ALL_ACTIVE_USERS` stores the `ALL ACTIVE USERS`-related state, dependency, configuration, or result of `MatchType` (declared type `MatchType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ALL_ACTIVE_USERS` 时应保持 `MatchType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ALL_ACTIVE_USERS`, preserve `MatchType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ALL_ACTIVE_USERS,
        /**
         * 字段 `POSITION` 表示 `MatchType` 中与 `POSITION` 相关的状态、依赖、配置或结果（声明类型 `MatchType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `POSITION` stores the `POSITION`-related state, dependency, configuration, or result of `MatchType` (declared type `MatchType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `POSITION` 时应保持 `MatchType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `POSITION`, preserve `MatchType`'s lifecycle, immutability, and thread-safety constraints.
         */
        POSITION
    }

    /**
     * 类型 `Status` 位于 `AutoAssignmentRuleEntity` 内，是枚举，用于承载 `Status` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Status` is an enum inside `AutoAssignmentRuleEntity` and carries the responsibility, state, or contract for `Status`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Status` 作为 `AutoAssignmentRuleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Status` as the responsibility boundary of `AutoAssignmentRuleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
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
