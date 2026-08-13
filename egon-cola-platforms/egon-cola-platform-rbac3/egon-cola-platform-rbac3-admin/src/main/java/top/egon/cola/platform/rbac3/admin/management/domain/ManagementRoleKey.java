package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
     * 类型 `ManagementRoleKey` 位于 `ManagementRolePO` 内，是记录类型，用于承载 `ManagementRoleKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagementRoleKey` is a record inside `ManagementRolePO` and carries the responsibility, state, or contract for `ManagementRoleKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagementRoleKey` 作为 `ManagementRolePO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagementRoleKey` as the responsibility boundary of `ManagementRolePO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagementRoleKey(/**
 * 字段 `tenantId` 表示 `ManagementRoleKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long tenantId, /**
 * 字段 `policyId` 表示 `ManagementRoleKey` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long policyId, /**
 * 字段 `roleId` 表示 `ManagementRoleKey` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `ManagementRoleKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `roleId` 时应保持 `ManagementRoleKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `roleId`, preserve `ManagementRoleKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long roleId)
            implements Serializable {
    }
