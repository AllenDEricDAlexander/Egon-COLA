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
 * 类型 `ManagementSubjectEntity` 位于当前包内，是类型，用于承载 `Management Subject Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementSubjectEntity` is a type in its package and carries the responsibility, state, or contract for `Management Subject Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementSubjectEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementSubjectEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_management_subject")
@IdClass(ManagementSubjectEntity.Key.class)
public class ManagementSubjectEntity {

    /**
     * 字段 `tenantId` 表示 `ManagementSubjectEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementSubjectEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementSubjectEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementSubjectEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementSubjectEntity` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementSubjectEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementSubjectEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementSubjectEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `subjectType` 表示 `ManagementSubjectEntity` 中与 `subject Type` 相关的状态、依赖、配置或结果（声明类型 `SubjectType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `subjectType` stores the `subject Type`-related state, dependency, configuration, or result of `ManagementSubjectEntity` (declared type `SubjectType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `subjectType` 时应保持 `ManagementSubjectEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `subjectType`, preserve `ManagementSubjectEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private SubjectType subjectType;
    /**
     * 字段 `subjectId` 表示 `ManagementSubjectEntity` 中与 `subject Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `subjectId` stores the `subject Id`-related state, dependency, configuration, or result of `ManagementSubjectEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `subjectId` 时应保持 `ManagementSubjectEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `subjectId`, preserve `ManagementSubjectEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /**
     * 构造器 `ManagementSubjectEntity` 用于创建并初始化 `ManagementSubjectEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementSubjectEntity` creates and initializes `ManagementSubjectEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementSubjectEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementSubjectEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementSubjectEntity() {
    }

    /**
     * 构造器 `ManagementSubjectEntity` 用于创建并初始化 `ManagementSubjectEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementSubjectEntity` creates and initializes `ManagementSubjectEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementSubjectEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementSubjectEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectType 输入参数 `subjectType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementSubjectEntity(
            Long tenantId,
            Long policyId,
            SubjectType subjectType,
            Long subjectId
    ) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    /**
     * 类型 `Key` 位于 `ManagementSubjectEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `ManagementSubjectEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `ManagementSubjectEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `ManagementSubjectEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param subjectType 记录组件 `subjectType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjectType` carries constructor data whose meaning is defined by the record contract.
     * @param subjectId 记录组件 `subjectId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjectId` carries constructor data whose meaning is defined by the record contract.
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
             * 字段 `policyId` 表示 `Key` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long policyId,
            /**
             * 字段 `subjectType` 表示 `Key` 中与 `subject Type` 相关的状态、依赖、配置或结果（声明类型 `SubjectType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjectType` stores the `subject Type`-related state, dependency, configuration, or result of `Key` (declared type `SubjectType`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjectType` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjectType`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            SubjectType subjectType,
            /**
             * 字段 `subjectId` 表示 `Key` 中与 `subject Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjectId` stores the `subject Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjectId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjectId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long subjectId
    ) implements Serializable {
    }

    /**
     * 类型 `SubjectType` 位于 `ManagementSubjectEntity` 内，是枚举，用于承载 `Subject Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SubjectType` is an enum inside `ManagementSubjectEntity` and carries the responsibility, state, or contract for `Subject Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SubjectType` 作为 `ManagementSubjectEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SubjectType` as the responsibility boundary of `ManagementSubjectEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum SubjectType {
        /**
         * 字段 `USER` 表示 `SubjectType` 中与 `USER` 相关的状态、依赖、配置或结果（声明类型 `SubjectType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `USER` stores the `USER`-related state, dependency, configuration, or result of `SubjectType` (declared type `SubjectType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `USER` 时应保持 `SubjectType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `USER`, preserve `SubjectType`'s lifecycle, immutability, and thread-safety constraints.
         */
        USER,
        /**
         * 字段 `ROLE` 表示 `SubjectType` 中与 `ROLE` 相关的状态、依赖、配置或结果（声明类型 `SubjectType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROLE` stores the `ROLE`-related state, dependency, configuration, or result of `SubjectType` (declared type `SubjectType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROLE` 时应保持 `SubjectType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROLE`, preserve `SubjectType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROLE,
        /**
         * 字段 `POSITION` 表示 `SubjectType` 中与 `POSITION` 相关的状态、依赖、配置或结果（声明类型 `SubjectType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `POSITION` stores the `POSITION`-related state, dependency, configuration, or result of `SubjectType` (declared type `SubjectType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `POSITION` 时应保持 `SubjectType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `POSITION`, preserve `SubjectType`'s lifecycle, immutability, and thread-safety constraints.
         */
        POSITION
    }
}
