package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementSubjectSubjectTypeEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementSubjectPO;

/**
     * 类型 `ManagementSubjectKey` 位于 `ManagementSubjectPO` 内，是记录类型，用于承载 `ManagementSubjectKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagementSubjectKey` is a record inside `ManagementSubjectPO` and carries the responsibility, state, or contract for `ManagementSubjectKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagementSubjectKey` 作为 `ManagementSubjectPO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagementSubjectKey` as the responsibility boundary of `ManagementSubjectPO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param subjectType 记录组件 `subjectType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjectType` carries constructor data whose meaning is defined by the record contract.
     * @param subjectId 记录组件 `subjectId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjectId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagementSubjectKey(
            /**
             * 字段 `tenantId` 表示 `ManagementSubjectKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementSubjectKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementSubjectKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementSubjectKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long tenantId,
            /**
             * 字段 `policyId` 表示 `ManagementSubjectKey` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementSubjectKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementSubjectKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementSubjectKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long policyId,
            /**
             * 字段 `subjectType` 表示 `ManagementSubjectKey` 中与 `subject Type` 相关的状态、依赖、配置或结果（声明类型 `ManagementSubjectSubjectTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjectType` stores the `subject Type`-related state, dependency, configuration, or result of `ManagementSubjectKey` (declared type `ManagementSubjectSubjectTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjectType` 时应保持 `ManagementSubjectKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjectType`, preserve `ManagementSubjectKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            ManagementSubjectSubjectTypeEnum subjectType,
            /**
             * 字段 `subjectId` 表示 `ManagementSubjectKey` 中与 `subject Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjectId` stores the `subject Id`-related state, dependency, configuration, or result of `ManagementSubjectKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjectId` 时应保持 `ManagementSubjectKey` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjectId`, preserve `ManagementSubjectKey`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long subjectId
    ) implements Serializable {
    }
