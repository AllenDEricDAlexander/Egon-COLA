package top.egon.cola.platform.rbac3.admin.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/**
 * 类型 `RoleInheritanceEntity` 位于当前包内，是类型，用于承载 `Role Inheritance Entity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleInheritanceEntity` is a type in its package and carries the responsibility, state, or contract for `Role Inheritance Entity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RoleInheritanceEntity` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RoleInheritanceEntity` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Entity
@Table(name = "rbac3_role_inheritance")
public class RoleInheritanceEntity extends TenantScopedPO {

    /**
     * 字段 `id` 表示 `RoleInheritanceEntity` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `id` stores the `id`-related state, dependency, configuration, or result of `RoleInheritanceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `id` 时应保持 `RoleInheritanceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `id`, preserve `RoleInheritanceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Id
    private Long id;
    /**
     * 字段 `applicationId` 表示 `RoleInheritanceEntity` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleInheritanceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleInheritanceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleInheritanceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /**
     * 字段 `seniorRoleId` 表示 `RoleInheritanceEntity` 中与 `senior Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `seniorRoleId` stores the `senior Role Id`-related state, dependency, configuration, or result of `RoleInheritanceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `seniorRoleId` 时应保持 `RoleInheritanceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `seniorRoleId`, preserve `RoleInheritanceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "senior_role_id", nullable = false)
    private Long seniorRoleId;
    /**
     * 字段 `juniorRoleId` 表示 `RoleInheritanceEntity` 中与 `junior Role Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `juniorRoleId` stores the `junior Role Id`-related state, dependency, configuration, or result of `RoleInheritanceEntity` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `juniorRoleId` 时应保持 `RoleInheritanceEntity` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `juniorRoleId`, preserve `RoleInheritanceEntity`'s lifecycle, immutability, and thread-safety constraints.
     */
    @Column(name = "junior_role_id", nullable = false)
    private Long juniorRoleId;

    /**
     * 构造器 `RoleInheritanceEntity` 用于创建并初始化 `RoleInheritanceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleInheritanceEntity` creates and initializes `RoleInheritanceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleInheritanceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleInheritanceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    protected RoleInheritanceEntity() {
    }

    /**
     * 构造器 `RoleInheritanceEntity` 用于创建并初始化 `RoleInheritanceEntity` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleInheritanceEntity` creates and initializes `RoleInheritanceEntity`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleInheritanceEntity` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleInheritanceEntity`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param seniorRoleId 输入参数 `seniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param juniorRoleId 输入参数 `juniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleInheritanceEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long seniorRoleId,
            Long juniorRoleId,
            String actorId,
            Instant now) {
        if (seniorRoleId.equals(juniorRoleId)) {
            throw new IllegalArgumentException("role inheritance must be distinct");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.seniorRoleId = Objects.requireNonNull(seniorRoleId, "seniorRoleId");
        this.juniorRoleId = Objects.requireNonNull(juniorRoleId, "juniorRoleId");
        markCreated(actorId, now);
    }

    /**
     * 方法 `getSeniorRoleId` 按照 `RoleInheritanceEntity` 的职责处理输入，完成 `get Senior Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getSeniorRoleId` processes its inputs according to `RoleInheritanceEntity`'s responsibility, performs the `get Senior Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getSeniorRoleId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getSeniorRoleId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getSeniorRoleId() {
        return seniorRoleId;
    }

    /**
     * 方法 `getJuniorRoleId` 按照 `RoleInheritanceEntity` 的职责处理输入，完成 `get Junior Role Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `getJuniorRoleId` processes its inputs according to `RoleInheritanceEntity`'s responsibility, performs the `get Junior Role Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `getJuniorRoleId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `getJuniorRoleId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Long getJuniorRoleId() {
        return juniorRoleId;
    }
}
