package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * 类型 `ManagementRoleEntity` 位于当前包内，是类型，用于承载 `Management Role Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementRoleEntity` is a type in its package and carries the responsibility, state, or contract for `Management Role Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementRoleEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementRoleEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_management_role")
@IdClass(ManagementRoleEntity.Key.class)
public class ManagementRoleEntity {

    /**
     * 字段 `tenantId` 表示 `ManagementRoleEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementRoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementRoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementRoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementRoleEntity` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementRoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementRoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementRoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `roleId` 表示 `ManagementRoleEntity` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `ManagementRoleEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `ManagementRoleEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `ManagementRoleEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 构造器 `ManagementRoleEntity` 用于创建并初始化 `ManagementRoleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementRoleEntity` creates and initializes `ManagementRoleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementRoleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementRoleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementRoleEntity() {
    }

    /**
     * 构造器 `ManagementRoleEntity` 用于创建并初始化 `ManagementRoleEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementRoleEntity` creates and initializes `ManagementRoleEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementRoleEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementRoleEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementRoleEntity(Long tenantId, Long policyId, Long roleId) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.roleId = roleId;
    }

    /**
     * 类型 `Key` 位于 `ManagementRoleEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `ManagementRoleEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `ManagementRoleEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `ManagementRoleEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Key(/**
 * 字段 `tenantId` 表示 `Key` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ Long tenantId, /**
 * 字段 `policyId` 表示 `Key` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `policyId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `policyId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ Long policyId, /**
 * 字段 `roleId` 表示 `Key` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `roleId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `roleId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ Long roleId)
            implements Serializable {
    }
}
