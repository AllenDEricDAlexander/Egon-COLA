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
 * 类型 `ManagementScopeEntity` 位于当前包内，是类型，用于承载 `Management Scope Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementScopeEntity` is a type in its package and carries the responsibility, state, or contract for `Management Scope Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementScopeEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementScopeEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_management_scope")
@IdClass(ManagementScopeEntity.Key.class)
public class ManagementScopeEntity {

    /**
     * 字段 `tenantId` 表示 `ManagementScopeEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ManagementScopeEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ManagementScopeEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ManagementScopeEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    /**
     * 字段 `policyId` 表示 `ManagementScopeEntity` 中与 `policy Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `policyId` stores the `policy Id`-related state, dependency, configuration, or result of `ManagementScopeEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `policyId` 时应保持 `ManagementScopeEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `policyId`, preserve `ManagementScopeEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    /**
     * 字段 `scopeType` 表示 `ManagementScopeEntity` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `ManagementScopeEntity` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `ManagementScopeEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `ManagementScopeEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;
    /**
     * 字段 `scopeReferenceId` 表示 `ManagementScopeEntity` 中与 `scope Reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `scopeReferenceId` stores the `scope Reference Id`-related state, dependency, configuration, or result of `ManagementScopeEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `scopeReferenceId` 时应保持 `ManagementScopeEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `scopeReferenceId`, preserve `ManagementScopeEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "scope_ref_id")
    private Long scopeReferenceId;

    /**
     * 构造器 `ManagementScopeEntity` 用于创建并初始化 `ManagementScopeEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementScopeEntity` creates and initializes `ManagementScopeEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementScopeEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementScopeEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected ManagementScopeEntity() {
    }

    /**
     * 构造器 `ManagementScopeEntity` 用于创建并初始化 `ManagementScopeEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementScopeEntity` creates and initializes `ManagementScopeEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementScopeEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementScopeEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param scopeReferenceId 输入参数 `scopeReferenceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementScopeEntity(
            Long tenantId,
            Long policyId,
            ScopeType scopeType,
            Long scopeReferenceId
    ) {
        if (scopeType == ScopeType.SELF_DEPT && scopeReferenceId != null
                || scopeType != ScopeType.SELF_DEPT && scopeReferenceId == null) {
            throw new IllegalArgumentException("invalid management scope");
        }
        this.tenantId = tenantId;
        this.policyId = policyId;
        this.scopeType = scopeType;
        this.scopeReferenceId = scopeReferenceId;
    }

    /**
     * 类型 `Key` 位于 `ManagementScopeEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `ManagementScopeEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `ManagementScopeEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `ManagementScopeEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param policyId 记录组件 `policyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeReferenceId 记录组件 `scopeReferenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeReferenceId` carries constructor data whose meaning is defined by the record contract.
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
             * 字段 `scopeType` 表示 `Key` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `Key` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            ScopeType scopeType,
            /**
             * 字段 `scopeReferenceId` 表示 `Key` 中与 `scope Reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeReferenceId` stores the `scope Reference Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeReferenceId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeReferenceId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long scopeReferenceId
    ) implements Serializable {
    }

    /**
     * 类型 `ScopeType` 位于 `ManagementScopeEntity` 内，是枚举，用于承载 `Scope Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ScopeType` is an enum inside `ManagementScopeEntity` and carries the responsibility, state, or contract for `Scope Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ScopeType` 作为 `ManagementScopeEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ScopeType` as the responsibility boundary of `ManagementScopeEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ScopeType {
        /**
         * 字段 `SELF_DEPT` 表示 `ScopeType` 中与 `SELF DEPT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SELF_DEPT` stores the `SELF DEPT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SELF_DEPT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SELF_DEPT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SELF_DEPT,
        /**
         * 字段 `DEPT` 表示 `ScopeType` 中与 `DEPT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPT` stores the `DEPT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPT,
        /**
         * 字段 `DEPT_TREE` 表示 `ScopeType` 中与 `DEPT TREE` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPT_TREE` stores the `DEPT TREE`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPT_TREE` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPT_TREE`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPT_TREE,
        /**
         * 字段 `ORG` 表示 `ScopeType` 中与 `ORG` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ORG` stores the `ORG`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ORG` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ORG`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ORG,
        /**
         * 字段 `ORG_TREE` 表示 `ScopeType` 中与 `ORG TREE` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ORG_TREE` stores the `ORG TREE`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ORG_TREE` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ORG_TREE`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ORG_TREE,
        /**
         * 字段 `CUSTOM_DEPT` 表示 `ScopeType` 中与 `CUSTOM DEPT` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CUSTOM_DEPT` stores the `CUSTOM DEPT`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CUSTOM_DEPT` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CUSTOM_DEPT`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        CUSTOM_DEPT,
        /**
         * 字段 `CUSTOM_USER` 表示 `ScopeType` 中与 `CUSTOM USER` 相关的状态、依赖、配置或结果（声明类型 `ScopeType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `CUSTOM_USER` stores the `CUSTOM USER`-related state, dependency, configuration, or result of `ScopeType` (declared type `ScopeType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `CUSTOM_USER` 时应保持 `ScopeType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `CUSTOM_USER`, preserve `ScopeType`'s lifecycle, immutability, and thread-safety constraints.
         */
        CUSTOM_USER
    }
}
