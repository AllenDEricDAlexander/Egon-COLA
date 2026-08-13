package top.egon.cola.platform.rbac3.admin.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * 类型 `ManagementOperationEntity` 位于当前包内，是类型，用于承载 `Management Operation Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementOperationEntity` is a type in its package and carries the responsibility, state, or contract for `Management Operation Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementOperationEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementOperationEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_management_operation")
@IdClass(ManagementOperationEntity.Key.class)
public class ManagementOperationEntity {

    /**
     * 字段 `tenantId` 表示 `ManagementOperationEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementOperationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementOperationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementOperationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementOperationEntity` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementOperationEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementOperationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementOperationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `operation` 表示 `ManagementOperationEntity` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `ManagementOperationEntity` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `operation` 时应保持 `ManagementOperationEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `operation`, preserve `ManagementOperationEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", nullable = false, length = 32)
    private Operation operation;

    /**
     * 构造器 `ManagementOperationEntity` 用于创建并初始化 `ManagementOperationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementOperationEntity` creates and initializes `ManagementOperationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementOperationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementOperationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementOperationEntity() {
    }

    /**
     * 构造器 `ManagementOperationEntity` 用于创建并初始化 `ManagementOperationEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementOperationEntity` creates and initializes `ManagementOperationEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementOperationEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementOperationEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementOperationEntity(Long tenantId, Long policyId, Operation operation) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.operation = operation;
    }

    /**
     * 类型 `Key` 位于 `ManagementOperationEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `ManagementOperationEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `ManagementOperationEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `ManagementOperationEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param operation 记录组件 `operation` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operation` carries constructor data whose meaning is defined by the record contract.
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
 * 字段 `operation` 表示 `Key` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `Key` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `operation` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `operation`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
 */ Operation operation)
            implements Serializable {
    }

    /**
     * 类型 `Operation` 位于 `ManagementOperationEntity` 内，是枚举，用于承载 `Operation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Operation` is an enum inside `ManagementOperationEntity` and carries the responsibility, state, or contract for `Operation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Operation` 作为 `ManagementOperationEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Operation` as the responsibility boundary of `ManagementOperationEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum Operation {
        /**
         * 字段 `VIEW_ASSIGNMENT` 表示 `Operation` 中与 `VIEW ASSIGNMENT` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `VIEW_ASSIGNMENT` stores the `VIEW ASSIGNMENT`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `VIEW_ASSIGNMENT` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `VIEW_ASSIGNMENT`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        VIEW_ASSIGNMENT,
        /**
         * 字段 `ASSIGN_ROLE` 表示 `Operation` 中与 `ASSIGN ROLE` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ASSIGN_ROLE` stores the `ASSIGN ROLE`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ASSIGN_ROLE` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ASSIGN_ROLE`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        ASSIGN_ROLE,
        /**
         * 字段 `REVOKE_ROLE` 表示 `Operation` 中与 `REVOKE ROLE` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `REVOKE_ROLE` stores the `REVOKE ROLE`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `REVOKE_ROLE` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `REVOKE_ROLE`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        REVOKE_ROLE,
        /**
         * 字段 `SUSPEND_ROLE` 表示 `Operation` 中与 `SUSPEND ROLE` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SUSPEND_ROLE` stores the `SUSPEND ROLE`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SUSPEND_ROLE` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SUSPEND_ROLE`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        SUSPEND_ROLE,
        /**
         * 字段 `RESUME_ROLE` 表示 `Operation` 中与 `RESUME ROLE` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `RESUME_ROLE` stores the `RESUME ROLE`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `RESUME_ROLE` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `RESUME_ROLE`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        RESUME_ROLE,
        /**
         * 字段 `TEMPORARY_ASSIGN` 表示 `Operation` 中与 `TEMPORARY ASSIGN` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `TEMPORARY_ASSIGN` stores the `TEMPORARY ASSIGN`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `TEMPORARY_ASSIGN` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `TEMPORARY_ASSIGN`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        TEMPORARY_ASSIGN,
        /**
         * 字段 `VIEW_AUDIT` 表示 `Operation` 中与 `VIEW AUDIT` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `VIEW_AUDIT` stores the `VIEW AUDIT`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `VIEW_AUDIT` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `VIEW_AUDIT`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        VIEW_AUDIT,
        /**
         * 字段 `VIEW_IMPACT` 表示 `Operation` 中与 `VIEW IMPACT` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `VIEW_IMPACT` stores the `VIEW IMPACT`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `VIEW_IMPACT` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `VIEW_IMPACT`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        VIEW_IMPACT,
        /**
         * 字段 `SELF_REVOKE_LOW_RISK` 表示 `Operation` 中与 `SELF REVOKE LOW RISK` 相关的状态、依赖、配置或结果（声明类型 `Operation`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SELF_REVOKE_LOW_RISK` stores the `SELF REVOKE LOW RISK`-related state, dependency, configuration, or result of `Operation` (declared type `Operation`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SELF_REVOKE_LOW_RISK` 时应保持 `Operation` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SELF_REVOKE_LOW_RISK`, preserve `Operation`'s lifecycle, immutability, and thread-safety constraints.
         */
        SELF_REVOKE_LOW_RISK
    }
}
