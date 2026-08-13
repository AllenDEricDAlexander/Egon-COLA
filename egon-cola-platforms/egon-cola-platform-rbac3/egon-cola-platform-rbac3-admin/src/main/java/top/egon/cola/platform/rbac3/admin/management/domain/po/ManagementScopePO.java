package top.egon.cola.platform.rbac3.admin.management.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.management.domain.enums.ManagementScopeScopeTypeEnum;
import top.egon.cola.platform.rbac3.admin.management.domain.ManagementScopeKey;

/**
 * 类型 `ManagementScopePO` 位于当前包内，是类型，用于承载 `Management Scope Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementScopePO` is a type in its package and carries the responsibility, state, or contract for `Management Scope Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementScopePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementScopePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "ManagementScopeEntity")
@Table(name = "rbac3_management_scope")
@IdClass(ManagementScopeKey.class)
public class ManagementScopePO {

    /**
     * 字段 `tenantId` 表示 `ManagementScopePO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementScopePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementScopePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementScopePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementScopePO` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementScopePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementScopePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementScopePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `scopeType` 表示 `ManagementScopePO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ManagementScopeScopeTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `ManagementScopePO` (declared type `ManagementScopeScopeTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `ManagementScopePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `ManagementScopePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ManagementScopeScopeTypeEnum scopeType;
    /**
     * 字段 `scopeReferenceId` 表示 `ManagementScopePO` 中与 `scope Reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeReferenceId` stores the `scope Reference Id`-related state, dependency, configuration, or result of `ManagementScopePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeReferenceId` 时应保持 `ManagementScopePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeReferenceId`, preserve `ManagementScopePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "scope_ref_id")
    private Long scopeReferenceId;

    /**
     * 构造器 `ManagementScopePO` 用于创建并初始化 `ManagementScopePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementScopePO` creates and initializes `ManagementScopePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementScopePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementScopePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementScopePO() {
    }

    /**
     * 构造器 `ManagementScopePO` 用于创建并初始化 `ManagementScopePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementScopePO` creates and initializes `ManagementScopePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementScopePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementScopePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeReferenceId 输入参数 `scopeReferenceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementScopePO(
            Long tenantId,
            Long policyId,
            ManagementScopeScopeTypeEnum scopeType,
            Long scopeReferenceId
    ) {
        if (scopeType == ManagementScopeScopeTypeEnum.SELF_DEPT && scopeReferenceId != null
                || scopeType != ManagementScopeScopeTypeEnum.SELF_DEPT && scopeReferenceId == null) {
            throw new IllegalArgumentException("invalid management scope");
        }
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.scopeType = scopeType;
        this.scopeReferenceId = scopeReferenceId;
    }


    }
