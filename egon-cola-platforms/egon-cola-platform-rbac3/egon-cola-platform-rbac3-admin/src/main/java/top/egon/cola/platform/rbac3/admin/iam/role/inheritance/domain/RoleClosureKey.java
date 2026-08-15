package top.egon.cola.platform.rbac3.admin.iam.role.inheritance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
     * 类型 `RoleClosureKey` 位于 `RoleClosurePO` 内，是记录类型，用于承载 `RoleClosureKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleClosureKey` is a record inside `RoleClosurePO` and carries the responsibility, state, or contract for `RoleClosureKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleClosureKey` 作为 `RoleClosurePO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleClosureKey` as the responsibility boundary of `RoleClosurePO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param ancestorRoleId 记录组件 `ancestorRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ancestorRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param descendantRoleId 记录组件 `descendantRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `descendantRoleId` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleClosureKey(
            /**
             * 字段 `tenantId` 表示 `RoleClosureKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RoleClosureKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RoleClosureKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RoleClosureKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long tenantId,
            /**
             * 字段 `applicationId` 表示 `RoleClosureKey` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleClosureKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleClosureKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleClosureKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long applicationId,
            /**
             * 字段 `ancestorRoleId` 表示 `RoleClosureKey` 中与 `ancestor Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ancestorRoleId` stores the `ancestor Role Id`-related state, dependency, configuration, or result of `RoleClosureKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ancestorRoleId` 时应保持 `RoleClosureKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ancestorRoleId`, preserve `RoleClosureKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long ancestorRoleId,
            /**
             * 字段 `descendantRoleId` 表示 `RoleClosureKey` 中与 `descendant Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `descendantRoleId` stores the `descendant Role Id`-related state, dependency, configuration, or result of `RoleClosureKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `descendantRoleId` 时应保持 `RoleClosureKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `descendantRoleId`, preserve `RoleClosureKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long descendantRoleId
    ) implements Serializable {
    }
