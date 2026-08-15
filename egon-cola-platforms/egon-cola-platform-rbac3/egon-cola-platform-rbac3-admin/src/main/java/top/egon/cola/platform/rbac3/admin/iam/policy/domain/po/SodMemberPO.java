package top.egon.cola.platform.rbac3.admin.iam.policy.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import top.egon.cola.platform.rbac3.admin.iam.policy.domain.SodMemberKey;

/**
 * 类型 `SodMemberPO` 位于当前包内，是类型，用于承载 `Sod Member Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `SodMemberPO` is a type in its package and carries the responsibility, state, or contract for `Sod Member Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `SodMemberPO` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `SodMemberPO` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity(name = "SodMemberEntity")
@IdClass(SodMemberKey.class)
@Table(name = "rbac3_sod_member")
public class SodMemberPO {

    /**
     * 字段 `tenantId` 表示 `SodMemberPO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SodMemberPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SodMemberPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SodMemberPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    /**
     * 字段 `sodSetId` 表示 `SodMemberPO` 中与 `sod Set Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sodSetId` stores the `sod Set Id`-related state, dependency, configuration, or result of `SodMemberPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sodSetId` 时应保持 `SodMemberPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sodSetId`, preserve `SodMemberPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "sod_set_id")
    private Long sodSetId;
    /**
     * 字段 `roleId` 表示 `SodMemberPO` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `SodMemberPO` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleId` 时应保持 `SodMemberPO` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleId`, preserve `SodMemberPO`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 构造器 `SodMemberPO` 用于创建并初始化 `SodMemberPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SodMemberPO` creates and initializes `SodMemberPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SodMemberPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SodMemberPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected SodMemberPO() {
    }

    /**
     * 构造器 `SodMemberPO` 用于创建并初始化 `SodMemberPO` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `SodMemberPO` creates and initializes `SodMemberPO`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `SodMemberPO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `SodMemberPO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sodSetId 输入参数 `sodSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public SodMemberPO(Long tenantId, Long sodSetId, Long roleId) {
        this.tenantId = tenantId;
        this.sodSetId = sodSetId;
        this.roleId = roleId;
    }

    /**
     * 方法 `getRoleId` 按照 `SodMemberPO` 的职责处理输入，完成 `get Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getRoleId` processes its inputs according to `SodMemberPO`'s responsibility, performs the `get Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getRoleId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getRoleId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getRoleId() {
        return roleId;
    }

    }
