package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * 类型 `RoleClosureEntity` 位于当前包内，是类型，用于承载 `Role Closure Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleClosureEntity` is a type in its package and carries the responsibility, state, or contract for `Role Closure Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleClosureEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleClosureEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@IdClass(RoleClosureEntity.Key.class)
@Table(name = "rbac3_role_closure")
public class RoleClosureEntity {

    /**
     * 字段 `tenantId` 表示 `RoleClosureEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RoleClosureEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RoleClosureEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RoleClosureEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    /**
     * 字段 `applicationId` 表示 `RoleClosureEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleClosureEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleClosureEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleClosureEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "application_id")
    private Long applicationId;
    /**
     * 字段 `ancestorRoleId` 表示 `RoleClosureEntity` 中与 `ancestor Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ancestorRoleId` stores the `ancestor Role Id`-related state, dependency, configuration, or result of `RoleClosureEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ancestorRoleId` 时应保持 `RoleClosureEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ancestorRoleId`, preserve `RoleClosureEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "ancestor_role_id")
    private Long ancestorRoleId;
    /**
     * 字段 `descendantRoleId` 表示 `RoleClosureEntity` 中与 `descendant Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `descendantRoleId` stores the `descendant Role Id`-related state, dependency, configuration, or result of `RoleClosureEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `descendantRoleId` 时应保持 `RoleClosureEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `descendantRoleId`, preserve `RoleClosureEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "descendant_role_id")
    private Long descendantRoleId;
    /**
     * 字段 `depth` 表示 `RoleClosureEntity` 中与 `depth` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `depth` stores the `depth`-related state, dependency, configuration, or result of `RoleClosureEntity` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `depth` 时应保持 `RoleClosureEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `depth`, preserve `RoleClosureEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(nullable = false)
    private int depth;

    /**
     * 构造器 `RoleClosureEntity` 用于创建并初始化 `RoleClosureEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleClosureEntity` creates and initializes `RoleClosureEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleClosureEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleClosureEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RoleClosureEntity() {
    }

    /**
     * 类型 `Key` 位于 `RoleClosureEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `RoleClosureEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `RoleClosureEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `RoleClosureEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param ancestorRoleId 记录组件 `ancestorRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ancestorRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param descendantRoleId 记录组件 `descendantRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `descendantRoleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Key(
            /**
             * 字段 `tenantId` 表示 `Key` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long tenantId,
            /**
             * 字段 `applicationId` 表示 `Key` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long applicationId,
            /**
             * 字段 `ancestorRoleId` 表示 `Key` 中与 `ancestor Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ancestorRoleId` stores the `ancestor Role Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ancestorRoleId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ancestorRoleId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long ancestorRoleId,
            /**
             * 字段 `descendantRoleId` 表示 `Key` 中与 `descendant Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `descendantRoleId` stores the `descendant Role Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `descendantRoleId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `descendantRoleId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long descendantRoleId
    ) implements Serializable {
    }
}
