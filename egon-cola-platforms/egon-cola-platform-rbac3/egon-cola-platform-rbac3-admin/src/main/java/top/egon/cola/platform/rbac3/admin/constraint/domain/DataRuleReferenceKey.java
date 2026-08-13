package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleReferenceReferenceTypeEnum;

/**
     * 类型 `DataRuleReferenceKey` 位于 `DataRuleReferencePO` 内，是记录类型，用于承载 `DataRuleReferenceKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataRuleReferenceKey` is a record inside `DataRuleReferencePO` and carries the responsibility, state, or contract for `DataRuleReferenceKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataRuleReferenceKey` 作为 `DataRuleReferencePO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataRuleReferenceKey` as the responsibility boundary of `DataRuleReferencePO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param dataRuleId 记录组件 `dataRuleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `dataRuleId` carries constructor data whose meaning is defined by the record contract.
     * @param referenceType 记录组件 `referenceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceType` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record DataRuleReferenceKey(
            /**
             * 字段 `tenantId` 表示 `DataRuleReferenceKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `DataRuleReferenceKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `DataRuleReferenceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `DataRuleReferenceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long tenantId,
            /**
             * 字段 `dataRuleId` 表示 `DataRuleReferenceKey` 中与 `data Rule Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dataRuleId` stores the `data Rule Id`-related state, dependency, configuration, or result of `DataRuleReferenceKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dataRuleId` 时应保持 `DataRuleReferenceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dataRuleId`, preserve `DataRuleReferenceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long dataRuleId,
            /**
             * 字段 `referenceType` 表示 `DataRuleReferenceKey` 中与 `reference Type` 相关的状态、依赖、配置或结果（声明类型 `DataRuleReferenceReferenceTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `referenceType` stores the `reference Type`-related state, dependency, configuration, or result of `DataRuleReferenceKey` (declared type `DataRuleReferenceReferenceTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `referenceType` 时应保持 `DataRuleReferenceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `referenceType`, preserve `DataRuleReferenceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            DataRuleReferenceReferenceTypeEnum referenceType,
            /**
             * 字段 `referenceId` 表示 `DataRuleReferenceKey` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `DataRuleReferenceKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `DataRuleReferenceKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `DataRuleReferenceKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long referenceId) implements Serializable {
    }
