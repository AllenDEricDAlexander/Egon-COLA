package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.constraint.domain.po.SodMemberPO;

/**
     * 类型 `SodMemberKey` 位于 `SodMemberPO` 内，是记录类型，用于承载 `SodMemberKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodMemberKey` is a record inside `SodMemberPO` and carries the responsibility, state, or contract for `SodMemberKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodMemberKey` 作为 `SodMemberPO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodMemberKey` as the responsibility boundary of `SodMemberPO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param sodSetId 记录组件 `sodSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sodSetId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodMemberKey(/**
 * 字段 `tenantId` 表示 `SodMemberKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SodMemberKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SodMemberKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SodMemberKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long tenantId, /**
 * 字段 `sodSetId` 表示 `SodMemberKey` 中与 `sod Set Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `sodSetId` stores the `sod Set Id`-related state, dependency, configuration, or result of `SodMemberKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `sodSetId` 时应保持 `SodMemberKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `sodSetId`, preserve `SodMemberKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long sodSetId, /**
 * 字段 `roleId` 表示 `SodMemberKey` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `SodMemberKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `roleId` 时应保持 `SodMemberKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `roleId`, preserve `SodMemberKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long roleId) implements Serializable {
    }
