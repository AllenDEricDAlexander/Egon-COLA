package top.egon.cola.platform.rbac3.admin.management.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementSubjectSubjectTypeEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementSubjectKey;

/**
 * 类型 `ManagementSubjectPO` 位于当前包内，是类型，用于承载 `Management Subject Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementSubjectPO` is a type in its package and carries the responsibility, state, or contract for `Management Subject Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementSubjectPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementSubjectPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ManagementSubjectEntity")
@Table(name = "rbac3_management_subject")
@IdClass(ManagementSubjectKey.class)
public class ManagementSubjectPO {

    /**
     * 字段 `tenantId` 表示 `ManagementSubjectPO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementSubjectPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementSubjectPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementSubjectPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementSubjectPO` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementSubjectPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementSubjectPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementSubjectPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `subjectType` 表示 `ManagementSubjectPO` 中与 `subject Type` 相关的状态、依赖、配置或结果（声明类型 `ManagementSubjectSubjectTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `subjectType` stores the `subject Type`-related state, dependency, configuration, or result of `ManagementSubjectPO` (declared type `ManagementSubjectSubjectTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `subjectType` 时应保持 `ManagementSubjectPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `subjectType`, preserve `ManagementSubjectPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private ManagementSubjectSubjectTypeEnum subjectType;
    /**
     * 字段 `subjectId` 表示 `ManagementSubjectPO` 中与 `subject Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `subjectId` stores the `subject Id`-related state, dependency, configuration, or result of `ManagementSubjectPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `subjectId` 时应保持 `ManagementSubjectPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `subjectId`, preserve `ManagementSubjectPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /**
     * 构造器 `ManagementSubjectPO` 用于创建并初始化 `ManagementSubjectPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementSubjectPO` creates and initializes `ManagementSubjectPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementSubjectPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementSubjectPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementSubjectPO() {
    }

    /**
     * 构造器 `ManagementSubjectPO` 用于创建并初始化 `ManagementSubjectPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementSubjectPO` creates and initializes `ManagementSubjectPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementSubjectPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementSubjectPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectType 输入参数 `subjectType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param subjectId 输入参数 `subjectId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementSubjectPO(
            Long tenantId,
            Long policyId,
            ManagementSubjectSubjectTypeEnum subjectType,
            Long subjectId
    ) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }


    }
