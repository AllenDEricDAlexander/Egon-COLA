package top.egon.cola.platform.rbac3.admin.constraint.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.DataRuleReferenceReferenceTypeEnum;
import top.egon.cola.platform.rbac3.admin.constraint.domain.DataRuleReferenceKey;

/**
 * 类型 `DataRuleReferencePO` 位于当前包内，是类型，用于承载 `Data Rule Reference Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DataRuleReferencePO` is a type in its package and carries the responsibility, state, or contract for `Data Rule Reference Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `DataRuleReferencePO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `DataRuleReferencePO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "DataRuleReferenceEntity")
@IdClass(DataRuleReferenceKey.class)
@Table(name = "rbac3_data_rule_ref")
public class DataRuleReferencePO {

    /**
     * 字段 `tenantId` 表示 `DataRuleReferencePO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `DataRuleReferencePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `DataRuleReferencePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `DataRuleReferencePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * 字段 `dataRuleId` 表示 `DataRuleReferencePO` 中与 `data Rule Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `dataRuleId` stores the `data Rule Id`-related state, dependency, configuration, or result of `DataRuleReferencePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `dataRuleId` 时应保持 `DataRuleReferencePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `dataRuleId`, preserve `DataRuleReferencePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "data_rule_id")
    private Long dataRuleId;

    /**
     * 字段 `referenceType` 表示 `DataRuleReferencePO` 中与 `reference Type` 相关的状态、依赖、配置或结果（声明类型 `DataRuleReferenceReferenceTypeEnum`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `referenceType` stores the `reference Type`-related state, dependency, configuration, or result of `DataRuleReferencePO` (declared type `DataRuleReferenceReferenceTypeEnum`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `referenceType` 时应保持 `DataRuleReferencePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `referenceType`, preserve `DataRuleReferencePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 32)
    private DataRuleReferenceReferenceTypeEnum referenceType;

    /**
     * 字段 `referenceId` 表示 `DataRuleReferencePO` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `DataRuleReferencePO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `DataRuleReferencePO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `DataRuleReferencePO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "ref_id")
    private Long referenceId;

    /**
     * 构造器 `DataRuleReferencePO` 用于创建并初始化 `DataRuleReferencePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DataRuleReferencePO` creates and initializes `DataRuleReferencePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DataRuleReferencePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DataRuleReferencePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected DataRuleReferencePO() {
    }

    /**
     * 构造器 `DataRuleReferencePO` 用于创建并初始化 `DataRuleReferencePO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DataRuleReferencePO` creates and initializes `DataRuleReferencePO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DataRuleReferencePO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DataRuleReferencePO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param dataRuleId 输入参数 `dataRuleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param referenceType 输入参数 `referenceType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param referenceId 输入参数 `referenceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public DataRuleReferencePO(
            Long tenantId,
            Long dataRuleId,
            DataRuleReferenceReferenceTypeEnum referenceType,
            Long referenceId) {
        this.tenantId = tenantId;
        this.dataRuleId = dataRuleId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    /**
     * 方法 `getReferenceType` 按照 `DataRuleReferencePO` 的职责处理输入，完成 `get Reference Type` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReferenceType` processes its inputs according to `DataRuleReferencePO`'s responsibility, performs the `get Reference Type` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReferenceType` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReferenceType`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public DataRuleReferenceReferenceTypeEnum getReferenceType() {
        return referenceType;
    }

    /**
     * 方法 `getReferenceId` 按照 `DataRuleReferencePO` 的职责处理输入，完成 `get Reference Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getReferenceId` processes its inputs according to `DataRuleReferencePO`'s responsibility, performs the `get Reference Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getReferenceId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getReferenceId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getReferenceId() {
        return referenceId;
    }


    }
