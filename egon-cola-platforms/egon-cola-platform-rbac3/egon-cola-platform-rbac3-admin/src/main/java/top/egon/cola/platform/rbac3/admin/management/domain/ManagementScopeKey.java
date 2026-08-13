package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementScopeScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementScopePO;

/**
     * 类型 `ManagementScopeKey` 位于 `ManagementScopePO` 内，是记录类型，用于承载 `ManagementScopeKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagementScopeKey` is a record inside `ManagementScopePO` and carries the responsibility, state, or contract for `ManagementScopeKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagementScopeKey` 作为 `ManagementScopePO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagementScopeKey` as the responsibility boundary of `ManagementScopePO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeReferenceId 记录组件 `scopeReferenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeReferenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagementScopeKey(
            /**
             * 字段 `tenantId` 表示 `ManagementScopeKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementScopeKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementScopeKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementScopeKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long tenantId,
            /**
             * 字段 `policyId` 表示 `ManagementScopeKey` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementScopeKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementScopeKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementScopeKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long policyId,
            /**
             * 字段 `scopeType` 表示 `ManagementScopeKey` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ManagementScopeScopeTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `ManagementScopeKey` (declared type `ManagementScopeScopeTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `ManagementScopeKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `ManagementScopeKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            ManagementScopeScopeTypeEnum scopeType,
            /**
             * 字段 `scopeReferenceId` 表示 `ManagementScopeKey` 中与 `scope Reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeReferenceId` stores the `scope Reference Id`-related state, dependency, configuration, or result of `ManagementScopeKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeReferenceId` 时应保持 `ManagementScopeKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeReferenceId`, preserve `ManagementScopeKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long scopeReferenceId
    ) implements Serializable {
    }
