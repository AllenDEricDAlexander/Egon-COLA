package top.egon.cola.platform.rbac3.admin.management.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementOperationOperationEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementOperationKey;

/**
 * 类型 `ManagementOperationPO` 位于当前包内，是类型，用于承载 `Management ManagementOperationOperationEnum Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementOperationPO` is a type in its package and carries the responsibility, state, or contract for `Management ManagementOperationOperationEnum Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementOperationPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementOperationPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ManagementOperationEntity")
@Table(name = "rbac3_management_operation")
@IdClass(ManagementOperationKey.class)
public class ManagementOperationPO {

    /**
     * 字段 `tenantId` 表示 `ManagementOperationPO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementOperationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementOperationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementOperationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementOperationPO` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementOperationPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementOperationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementOperationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `operation` 表示 `ManagementOperationPO` 中与 `operation` 相关的状态、依赖、配置或结果（声明类型 `ManagementOperationOperationEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `operation` stores the `operation`-related state, dependency, configuration, or result of `ManagementOperationPO` (declared type `ManagementOperationOperationEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `operation` 时应保持 `ManagementOperationPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `operation`, preserve `ManagementOperationPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", nullable = false, length = 32)
    private ManagementOperationOperationEnum operation;

    /**
     * 构造器 `ManagementOperationPO` 用于创建并初始化 `ManagementOperationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementOperationPO` creates and initializes `ManagementOperationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementOperationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementOperationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementOperationPO() {
    }

    /**
     * 构造器 `ManagementOperationPO` 用于创建并初始化 `ManagementOperationPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementOperationPO` creates and initializes `ManagementOperationPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementOperationPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementOperationPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementOperationPO(Long tenantId, Long policyId, ManagementOperationOperationEnum operation) {
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.operation = operation;
    }


    }
