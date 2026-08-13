package top.egon.cola.platform.rbac3.admin.constraint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * 类型 `DataRuleReferenceEntity` 位于当前包内，是类型，用于承载 `Data Rule Reference Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DataRuleReferenceEntity` is a type in its package and carries the responsibility, state, or contract for `Data Rule Reference Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `DataRuleReferenceEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `DataRuleReferenceEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@IdClass(DataRuleReferenceEntity.Key.class)
@Table(name = "rbac3_data_rule_ref")
public class DataRuleReferenceEntity {

    /**
     * 字段 `tenantId` 表示 `DataRuleReferenceEntity` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `DataRuleReferenceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `DataRuleReferenceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `DataRuleReferenceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * 字段 `dataRuleId` 表示 `DataRuleReferenceEntity` 中与 `data Rule Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `dataRuleId` stores the `data Rule Id`-related state, dependency, configuration, or result of `DataRuleReferenceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `dataRuleId` 时应保持 `DataRuleReferenceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `dataRuleId`, preserve `DataRuleReferenceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "data_rule_id")
    private Long dataRuleId;

    /**
     * 字段 `referenceType` 表示 `DataRuleReferenceEntity` 中与 `reference Type` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `referenceType` stores the `reference Type`-related state, dependency, configuration, or result of `DataRuleReferenceEntity` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `referenceType` 时应保持 `DataRuleReferenceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `referenceType`, preserve `DataRuleReferenceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 32)
    private ReferenceType referenceType;

    /**
     * 字段 `referenceId` 表示 `DataRuleReferenceEntity` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `DataRuleReferenceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `DataRuleReferenceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `DataRuleReferenceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "ref_id")
    private Long referenceId;

    /**
     * 构造器 `DataRuleReferenceEntity` 用于创建并初始化 `DataRuleReferenceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DataRuleReferenceEntity` creates and initializes `DataRuleReferenceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DataRuleReferenceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DataRuleReferenceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected DataRuleReferenceEntity() {
    }

    /**
     * 构造器 `DataRuleReferenceEntity` 用于创建并初始化 `DataRuleReferenceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DataRuleReferenceEntity` creates and initializes `DataRuleReferenceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DataRuleReferenceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DataRuleReferenceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param dataRuleId 输入参数 `dataRuleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param referenceType 输入参数 `referenceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param referenceId 输入参数 `referenceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DataRuleReferenceEntity(
            Long tenantId,
            Long dataRuleId,
            ReferenceType referenceType,
            Long referenceId) {
        this.tenantId = tenantId;
        this.dataRuleId = dataRuleId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    /**
     * 方法 `getReferenceType` 按照 `DataRuleReferenceEntity` 的职责处理输入，完成 `get Reference Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReferenceType` processes its inputs according to `DataRuleReferenceEntity`'s responsibility, performs the `get Reference Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReferenceType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReferenceType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ReferenceType getReferenceType() {
        return referenceType;
    }

    /**
     * 方法 `getReferenceId` 按照 `DataRuleReferenceEntity` 的职责处理输入，完成 `get Reference Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReferenceId` processes its inputs according to `DataRuleReferenceEntity`'s responsibility, performs the `get Reference Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReferenceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReferenceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getReferenceId() {
        return referenceId;
    }

    /**
     * 类型 `ReferenceType` 位于 `DataRuleReferenceEntity` 内，是枚举，用于承载 `Reference Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ReferenceType` is an enum inside `DataRuleReferenceEntity` and carries the responsibility, state, or contract for `Reference Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ReferenceType` 作为 `DataRuleReferenceEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ReferenceType` as the responsibility boundary of `DataRuleReferenceEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ReferenceType {
        /**
         * 字段 `USER` 表示 `ReferenceType` 中与 `USER` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `USER` stores the `USER`-related state, dependency, configuration, or result of `ReferenceType` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `USER` 时应保持 `ReferenceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `USER`, preserve `ReferenceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        USER,
        /**
         * 字段 `DEPT` 表示 `ReferenceType` 中与 `DEPT` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DEPT` stores the `DEPT`-related state, dependency, configuration, or result of `ReferenceType` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DEPT` 时应保持 `ReferenceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DEPT`, preserve `ReferenceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DEPT,
        /**
         * 字段 `ORG` 表示 `ReferenceType` 中与 `ORG` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ORG` stores the `ORG`-related state, dependency, configuration, or result of `ReferenceType` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ORG` 时应保持 `ReferenceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ORG`, preserve `ReferenceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        ORG,
        /**
         * 字段 `POSITION` 表示 `ReferenceType` 中与 `POSITION` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `POSITION` stores the `POSITION`-related state, dependency, configuration, or result of `ReferenceType` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `POSITION` 时应保持 `ReferenceType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `POSITION`, preserve `ReferenceType`'s lifecycle, immutability, and thread-safety constraints.
         */
        POSITION
    }

    /**
     * 类型 `Key` 位于 `DataRuleReferenceEntity` 内，是记录类型，用于承载 `Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Key` is a record inside `DataRuleReferenceEntity` and carries the responsibility, state, or contract for `Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Key` 作为 `DataRuleReferenceEntity` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Key` as the responsibility boundary of `DataRuleReferenceEntity`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param dataRuleId 记录组件 `dataRuleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `dataRuleId` carries constructor data whose meaning is defined by the record contract.
     * @param referenceType 记录组件 `referenceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceType` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
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
             * 字段 `dataRuleId` 表示 `Key` 中与 `data Rule Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dataRuleId` stores the `data Rule Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dataRuleId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dataRuleId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long dataRuleId,
            /**
             * 字段 `referenceType` 表示 `Key` 中与 `reference Type` 相关的状态、依赖、配置或结果（声明类型 `ReferenceType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `referenceType` stores the `reference Type`-related state, dependency, configuration, or result of `Key` (declared type `ReferenceType`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `referenceType` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `referenceType`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            ReferenceType referenceType,
            /**
             * 字段 `referenceId` 表示 `Key` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `Key` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `Key` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `Key`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long referenceId) implements Serializable {
    }
}
