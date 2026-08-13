package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementOperationOperationEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.po.ManagementOperationPO;

/**
     * 类型 `ManagementOperationKey` 位于 `ManagementOperationPO` 内，是记录类型，用于承载 `ManagementOperationKey` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManagementOperationKey` is a record inside `ManagementOperationPO` and carries the responsibility, state, or contract for `ManagementOperationKey`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManagementOperationKey` 作为 `ManagementOperationPO` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManagementOperationKey` as the responsibility boundary of `ManagementOperationPO`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param operation 记录组件 `operation` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operation` carries constructor data whose meaning is defined by the record contract.
     */
    public record ManagementOperationKey(/**
 * 字段 `tenantId` 表示 `ManagementOperationKey` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementOperationKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementOperationKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementOperationKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long tenantId, /**
 * 字段 `policyId` 表示 `ManagementOperationKey` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementOperationKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementOperationKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementOperationKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long policyId, /**
 * 字段 `operation` 表示 `ManagementOperationKey` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `ManagementOperationOperationEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `ManagementOperationKey` (declared type `ManagementOperationOperationEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `operation` 时应保持 `ManagementOperationKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `operation`, preserve `ManagementOperationKey`'s lifecycle, immutability, and thread-safety constraints.
 */ ManagementOperationOperationEnum operation)
            implements Serializable {
    }
