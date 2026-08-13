package top.egon.cola.platform.rbac3.admin.role.application;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;

/**
 * 类型 `RoleFacade` 位于当前包内，是类型，用于承载 `Role Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleFacade` is a type in its package and carries the responsibility, state, or contract for `Role Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Serializes role graph changes and rebuilds the materialized closure atomically.
 */
public final class RoleFacade {

    /**
     * 字段 `hierarchyStore` 表示 `RoleFacade` 中与 `hierarchy Store` 相关的状态、依赖、配置或结果（声明类型 `HierarchyStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `hierarchyStore` stores the `hierarchy Store`-related state, dependency, configuration, or result of `RoleFacade` (declared type `HierarchyStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `hierarchyStore` 时应保持 `RoleFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `hierarchyStore`, preserve `RoleFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final HierarchyStore hierarchyStore;
    /**
     * 字段 `roleControlStore` 表示 `RoleFacade` 中与 `role Control Store` 相关的状态、依赖、配置或结果（声明类型 `RoleControlStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleControlStore` stores the `role Control Store`-related state, dependency, configuration, or result of `RoleFacade` (declared type `RoleControlStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleControlStore` 时应保持 `RoleFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleControlStore`, preserve `RoleFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleControlStore roleControlStore;
    /**
     * 字段 `validator` 表示 `RoleFacade` 中与 `validator` 相关的状态、依赖、配置或结果（声明类型 `RoleHierarchyValidator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `validator` stores the `validator`-related state, dependency, configuration, or result of `RoleFacade` (declared type `RoleHierarchyValidator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `validator` 时应保持 `RoleFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `validator`, preserve `RoleFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleHierarchyValidator validator = new RoleHierarchyValidator();

    /**
     * 构造器 `RoleFacade` 用于创建并初始化 `RoleFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleFacade` creates and initializes `RoleFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param hierarchyStore 输入参数 `hierarchyStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleFacade(HierarchyStore hierarchyStore) {
        this(hierarchyStore, null);
    }

    /**
     * 构造器 `RoleFacade` 用于创建并初始化 `RoleFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleFacade` creates and initializes `RoleFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param hierarchyStore 输入参数 `hierarchyStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleControlStore 输入参数 `roleControlStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleFacade(HierarchyStore hierarchyStore, RoleControlStore roleControlStore) {
        this.hierarchyStore = Objects.requireNonNull(hierarchyStore, "hierarchyStore");
        this.roleControlStore = roleControlStore;
    }

    /**
     * 方法 `createRole` 按照 `RoleFacade` 的职责处理输入，完成 `create Role` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createRole` processes its inputs according to `RoleFacade`'s responsibility, performs the `create Role` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createRole` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createRole`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleMutationResult createRole(CreateRoleCommand command, Instant now) {
        return requiredControlStore().create(command, now);
    }

    /**
     * 方法 `assignPermission` 按照 `RoleFacade` 的职责处理输入，完成 `assign Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignPermission` processes its inputs according to `RoleFacade`'s responsibility, performs the `assign Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleMutationResult assignPermission(
            AssignPermissionCommand command,
            Instant now) {
        return requiredControlStore().assignPermission(command, now);
    }

    /**
     * 方法 `assignPermissions` 按照 `RoleFacade` 的职责处理输入，完成 `assign Permissions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignPermissions` processes its inputs according to `RoleFacade`'s responsibility, performs the `assign Permissions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignPermissions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignPermissions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleMutationResult assignPermissions(
            AssignPermissionsCommand command,
            Instant now) {
        return requiredControlStore().assignPermissions(command, now);
    }

    /**
     * 方法 `removePermission` 按照 `RoleFacade` 的职责处理输入，完成 `remove Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removePermission` processes its inputs according to `RoleFacade`'s responsibility, performs the `remove Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleMutationResult removePermission(
            RemovePermissionCommand command,
            Instant now) {
        return requiredControlStore().removePermission(command, now);
    }

    /**
     * 方法 `updateRole` 按照 `RoleFacade` 的职责处理输入，完成 `update Role` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updateRole` processes its inputs according to `RoleFacade`'s responsibility, performs the `update Role` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updateRole` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updateRole`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleMutationResult updateRole(UpdateRoleCommand command, Instant now) {
        return requiredControlStore().update(command, now);
    }

    /**
     * 方法 `roles` 按照 `RoleFacade` 的职责处理输入，完成 `roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roles` processes its inputs according to `RoleFacade`'s responsibility, performs the `roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<RoleView> roles(String tenantId, String applicationId) {
        return List.copyOf(requiredControlStore().roles(tenantId, applicationId));
    }

    /**
     * 方法 `impact` 按照 `RoleFacade` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `impact` processes its inputs according to `RoleFacade`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleImpactView impact(String tenantId, String roleId) {
        return requiredControlStore().impact(tenantId, roleId);
    }

    /**
     * 方法 `addInheritance` 按照 `RoleFacade` 的职责处理输入，完成 `add Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `addInheritance` processes its inputs according to `RoleFacade`'s responsibility, performs the `add Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `addInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `addInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param seniorRoleId 输入参数 `seniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param juniorRoleId 输入参数 `juniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void addInheritance(
            String tenantId,
            String applicationId,
            String seniorRoleId,
            String juniorRoleId) {
        addInheritance(new InheritanceCommand(
                tenantId, applicationId, seniorRoleId, juniorRoleId,
                -1L, "role-control-plane"));
    }

    /**
     * 方法 `addInheritance` 按照 `RoleFacade` 的职责处理输入，完成 `add Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `addInheritance` processes its inputs according to `RoleFacade`'s responsibility, performs the `add Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `addInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `addInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void addInheritance(InheritanceCommand command) {
        mutateInheritance(command, true);
    }

    /**
     * 方法 `mutateInheritance` 按照 `RoleFacade` 的职责处理输入，完成 `mutate Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mutateInheritance` processes its inputs according to `RoleFacade`'s responsibility, performs the `mutate Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mutateInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mutateInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param add 输入参数 `add`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void mutateInheritance(InheritanceCommand command, boolean add) {
        String tenantId = command.tenantId();
        String applicationId = command.applicationId();
        String seniorRoleId = command.seniorRoleId();
        String juniorRoleId = command.juniorRoleId();
        RoleEdge edge = new RoleEdge(seniorRoleId, juniorRoleId);
        hierarchyStore.withGraphLock(tenantId, applicationId, current -> {
            hierarchyStore.assertRoleVersion(
                    tenantId, seniorRoleId, command.expectedRoleVersion());
            if (add && current.edges().contains(edge)
                    || !add && !current.edges().contains(edge)) {
                return null;
            }
            var edges = new ArrayList<>(current.edges());
            if (add) {
                edges.add(edge);
            } else {
                edges.remove(edge);
            }
            RoleHierarchy next = new RoleHierarchy(current.nodes().values(), edges);
            validate(next);
            if (add) {
                hierarchyStore.addEdge(tenantId, applicationId, edge);
            } else {
                hierarchyStore.removeEdge(tenantId, applicationId, edge);
            }
            hierarchyStore.rebuildClosure(tenantId, applicationId);
            hierarchyStore.recordGraphMutation(
                    tenantId, applicationId, edge, add, command.actorId());
            return null;
        });
    }

    /**
     * 方法 `removeInheritance` 按照 `RoleFacade` 的职责处理输入，完成 `remove Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removeInheritance` processes its inputs according to `RoleFacade`'s responsibility, performs the `remove Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removeInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removeInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param seniorRoleId 输入参数 `seniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param juniorRoleId 输入参数 `juniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void removeInheritance(
            String tenantId,
            String applicationId,
            String seniorRoleId,
            String juniorRoleId) {
        removeInheritance(new InheritanceCommand(
                tenantId, applicationId, seniorRoleId, juniorRoleId,
                -1L, "role-control-plane"));
    }

    /**
     * 方法 `removeInheritance` 按照 `RoleFacade` 的职责处理输入，完成 `remove Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removeInheritance` processes its inputs according to `RoleFacade`'s responsibility, performs the `remove Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removeInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removeInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void removeInheritance(InheritanceCommand command) {
        mutateInheritance(command, false);
    }

    /**
     * 方法 `validate` 按照 `RoleFacade` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `RoleFacade`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param hierarchy 输入参数 `hierarchy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validate(RoleHierarchy hierarchy) {
        validator.validate(hierarchy);
        for (String roleId : hierarchy.nodes().keySet()) {
            if (hierarchy.rootsOf(roleId).size() != 1) {
                throw new Rbac3RuleViolation(
                        "ROLE_ACTIVATION_ROOT_AMBIGUOUS", java.util.List.of(roleId));
            }
        }
    }

    /**
     * 方法 `requiredControlStore` 按照 `RoleFacade` 的职责处理输入，完成 `required Control Store` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredControlStore` processes its inputs according to `RoleFacade`'s responsibility, performs the `required Control Store` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredControlStore` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredControlStore`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RoleControlStore requiredControlStore() {
        if (roleControlStore == null) {
            throw new IllegalStateException("role control store is not configured");
        }
        return roleControlStore;
    }

    /**
     * 类型 `HierarchyStore` 位于 `RoleFacade` 内，是接口，用于承载 `Hierarchy Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `HierarchyStore` is an interface inside `RoleFacade` and carries the responsibility, state, or contract for `Hierarchy Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `HierarchyStore` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `HierarchyStore` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface HierarchyStore {

        /**
         * 方法 `withGraphLock` 按照 `HierarchyStore` 的职责处理输入，完成 `with Graph Lock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `withGraphLock` processes its inputs according to `HierarchyStore`'s responsibility, performs the `with Graph Lock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `withGraphLock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `withGraphLock`, then continue the business flow using its result, exception, or side effect.
         *
         * @param <T> 类型参数表示图锁回调结果的具体类型；type parameter representing the graph-lock callback result type.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param action 输入参数 `action`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        <T> T withGraphLock(
                String tenantId,
                String applicationId,
                Function<RoleHierarchy, T> action);

        /**
         * 方法 `addEdge` 按照 `HierarchyStore` 的职责处理输入，完成 `add Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `addEdge` processes its inputs according to `HierarchyStore`'s responsibility, performs the `add Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `addEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `addEdge`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void addEdge(String tenantId, String applicationId, RoleEdge edge);

        /**
         * 方法 `removeEdge` 按照 `HierarchyStore` 的职责处理输入，完成 `remove Edge` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `removeEdge` processes its inputs according to `HierarchyStore`'s responsibility, performs the `remove Edge` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `removeEdge` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `removeEdge`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void removeEdge(String tenantId, String applicationId, RoleEdge edge);

        /**
         * 方法 `rebuildClosure` 按照 `HierarchyStore` 的职责处理输入，完成 `rebuild Closure` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rebuildClosure` processes its inputs according to `HierarchyStore`'s responsibility, performs the `rebuild Closure` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rebuildClosure` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rebuildClosure`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void rebuildClosure(String tenantId, String applicationId);

        /**
         * 方法 `assertRoleVersion` 按照 `HierarchyStore` 的职责处理输入，完成 `assert Role Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assertRoleVersion` processes its inputs according to `HierarchyStore`'s responsibility, performs the `assert Role Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assertRoleVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assertRoleVersion`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void assertRoleVersion(
                String tenantId,
                String roleId,
                long expectedRoleVersion) {
        }

        /**
         * 方法 `recordGraphMutation` 按照 `HierarchyStore` 的职责处理输入，完成 `record Graph Mutation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `recordGraphMutation` processes its inputs according to `HierarchyStore`'s responsibility, performs the `record Graph Mutation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `recordGraphMutation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `recordGraphMutation`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param edge 输入参数 `edge`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param added 输入参数 `added`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        default void recordGraphMutation(
                String tenantId,
                String applicationId,
                RoleEdge edge,
                boolean added,
                String actorId) {
        }
    }

    /**
     * 类型 `RoleControlStore` 位于 `RoleFacade` 内，是接口，用于承载 `Role Control Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleControlStore` is an interface inside `RoleFacade` and carries the responsibility, state, or contract for `Role Control Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleControlStore` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleControlStore` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface RoleControlStore {

        /**
         * 方法 `create` 按照 `RoleControlStore` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `create` processes its inputs according to `RoleControlStore`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleMutationResult create(CreateRoleCommand command, Instant now);

        /**
         * 方法 `assignPermission` 按照 `RoleControlStore` 的职责处理输入，完成 `assign Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assignPermission` processes its inputs according to `RoleControlStore`'s responsibility, performs the `assign Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assignPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assignPermission`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleMutationResult assignPermission(AssignPermissionCommand command, Instant now);

        /**
         * 方法 `assignPermissions` 按照 `RoleControlStore` 的职责处理输入，完成 `assign Permissions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `assignPermissions` processes its inputs according to `RoleControlStore`'s responsibility, performs the `assign Permissions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `assignPermissions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `assignPermissions`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResult assignPermissions(
                AssignPermissionsCommand command,
                Instant now) {
            RoleMutationResult result = null;
            for (String permissionId : command.permissionIds()) {
                result = assignPermission(new AssignPermissionCommand(
                        command.tenantId(),
                        command.applicationId(),
                        command.roleId(),
                        permissionId,
                        command.validFrom(),
                        command.validTo(),
                        command.actorId()), now);
            }
            return result;
        }

        /**
         * 方法 `removePermission` 按照 `RoleControlStore` 的职责处理输入，完成 `remove Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `removePermission` processes its inputs according to `RoleControlStore`'s responsibility, performs the `remove Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `removePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `removePermission`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResult removePermission(
                RemovePermissionCommand command,
                Instant now) {
            throw new UnsupportedOperationException("permission removal is not configured");
        }

        /**
         * 方法 `update` 按照 `RoleControlStore` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `update` processes its inputs according to `RoleControlStore`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleMutationResult update(UpdateRoleCommand command, Instant now) {
            throw new UnsupportedOperationException("role update is not configured");
        }

        /**
         * 方法 `roles` 按照 `RoleControlStore` 的职责处理输入，完成 `roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `roles` processes its inputs according to `RoleControlStore`'s responsibility, performs the `roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `roles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `roles`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default List<RoleView> roles(String tenantId, String applicationId) {
            throw new UnsupportedOperationException("role query is not configured");
        }

        /**
         * 方法 `impact` 按照 `RoleControlStore` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `impact` processes its inputs according to `RoleControlStore`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default RoleImpactView impact(String tenantId, String roleId) {
            throw new UnsupportedOperationException("role impact is not configured");
        }
    }

    /**
     * 类型 `CreateRoleCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Create Role Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CreateRoleCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Create Role Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CreateRoleCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CreateRoleCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record CreateRoleCommand(
            /**
             * 字段 `tenantId` 表示 `CreateRoleCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `CreateRoleCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleCode` 表示 `CreateRoleCommand` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `roleName` 表示 `CreateRoleCommand` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleName,
            /**
             * 字段 `roleType` 表示 `CreateRoleCommand` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleType,
            /**
             * 字段 `riskLevel` 表示 `CreateRoleCommand` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String riskLevel,
            /**
             * 字段 `privileged` 表示 `CreateRoleCommand` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `landingRouteId` 表示 `CreateRoleCommand` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `CreateRoleCommand` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `CreateRoleCommand` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `actorId` 表示 `CreateRoleCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `CreateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `CreateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `CreateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `AssignPermissionCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Assign Permission Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignPermissionCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Assign Permission Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignPermissionCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignPermissionCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignPermissionCommand(
            /**
             * 字段 `tenantId` 表示 `AssignPermissionCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `AssignPermissionCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `AssignPermissionCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `AssignPermissionCommand` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `validFrom` 表示 `AssignPermissionCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignPermissionCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `actorId` 表示 `AssignPermissionCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AssignPermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AssignPermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AssignPermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `AssignPermissionsCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Assign Permissions Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AssignPermissionsCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Assign Permissions Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AssignPermissionsCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AssignPermissionsCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionIds 记录组件 `permissionIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record AssignPermissionsCommand(
            /**
             * 字段 `tenantId` 表示 `AssignPermissionsCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `AssignPermissionsCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `AssignPermissionsCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionIds` 表示 `AssignPermissionsCommand` 中与 `permission Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionIds` stores the `permission Ids`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionIds` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionIds`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> permissionIds,
            /**
             * 字段 `validFrom` 表示 `AssignPermissionsCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validFrom,
            /**
             * 字段 `validTo` 表示 `AssignPermissionsCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedRoleVersion` 表示 `AssignPermissionsCommand` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedRoleVersion,
            /**
             * 字段 `actorId` 表示 `AssignPermissionsCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AssignPermissionsCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AssignPermissionsCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AssignPermissionsCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {

        /**
         * 构造器 `AssignPermissionsCommand` 用于创建并初始化 `AssignPermissionsCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AssignPermissionsCommand` creates and initializes `AssignPermissionsCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AssignPermissionsCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AssignPermissionsCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionIds 输入参数 `permissionIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AssignPermissionsCommand {
            permissionIds = List.copyOf(Objects.requireNonNull(
                    permissionIds, "permissionIds"));
            if (permissionIds.isEmpty()
                    || permissionIds.stream().distinct().count() != permissionIds.size()) {
                throw new IllegalArgumentException("permissionIds must be a non-empty set");
            }
            if (expectedRoleVersion < 0L) {
                throw new IllegalArgumentException("expectedRoleVersion must not be negative");
            }
        }
    }

    /**
     * 类型 `RemovePermissionCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Remove Permission Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RemovePermissionCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Remove Permission Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RemovePermissionCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RemovePermissionCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record RemovePermissionCommand(
            /**
             * 字段 `tenantId` 表示 `RemovePermissionCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `RemovePermissionCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `RemovePermissionCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `RemovePermissionCommand` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `expectedRoleVersion` 表示 `RemovePermissionCommand` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedRoleVersion,
            /**
             * 字段 `actorId` 表示 `RemovePermissionCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `RemovePermissionCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `RemovePermissionCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `RemovePermissionCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `UpdateRoleCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Update Role Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UpdateRoleCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Update Role Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UpdateRoleCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UpdateRoleCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record UpdateRoleCommand(
            /**
             * 字段 `tenantId` 表示 `UpdateRoleCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `roleId` 表示 `UpdateRoleCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `roleName` 表示 `UpdateRoleCommand` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleName,
            /**
             * 字段 `status` 表示 `UpdateRoleCommand` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `landingRouteId` 表示 `UpdateRoleCommand` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `UpdateRoleCommand` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `UpdateRoleCommand` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `expectedRoleVersion` 表示 `UpdateRoleCommand` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedRoleVersion,
            /**
             * 字段 `actorId` 表示 `UpdateRoleCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `UpdateRoleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `UpdateRoleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `UpdateRoleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `InheritanceCommand` 位于 `RoleFacade` 内，是记录类型，用于承载 `Inheritance Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `InheritanceCommand` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Inheritance Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `InheritanceCommand` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `InheritanceCommand` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param seniorRoleId 记录组件 `seniorRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `seniorRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param juniorRoleId 记录组件 `juniorRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `juniorRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record InheritanceCommand(
            /**
             * 字段 `tenantId` 表示 `InheritanceCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `InheritanceCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `seniorRoleId` 表示 `InheritanceCommand` 中与 `senior Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `seniorRoleId` stores the `senior Role Id`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `seniorRoleId` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `seniorRoleId`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String seniorRoleId,
            /**
             * 字段 `juniorRoleId` 表示 `InheritanceCommand` 中与 `junior Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `juniorRoleId` stores the `junior Role Id`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `juniorRoleId` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `juniorRoleId`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String juniorRoleId,
            /**
             * 字段 `expectedRoleVersion` 表示 `InheritanceCommand` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedRoleVersion,
            /**
             * 字段 `actorId` 表示 `InheritanceCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `InheritanceCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `InheritanceCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `InheritanceCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `RoleView` 位于 `RoleFacade` 内，是记录类型，用于承载 `Role View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleView` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Role View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleView` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleView` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleView(
            /**
             * 字段 `roleId` 表示 `RoleView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `applicationId` 表示 `RoleView` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleCode` 表示 `RoleView` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `roleName` 表示 `RoleView` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleName,
            /**
             * 字段 `roleType` 表示 `RoleView` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleType,
            /**
             * 字段 `riskLevel` 表示 `RoleView` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String riskLevel,
            /**
             * 字段 `privileged` 表示 `RoleView` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `RoleView` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `status` 表示 `RoleView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `RoleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `RoleView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `RoleView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `RoleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `RoleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }

    /**
     * 类型 `RoleImpactView` 位于 `RoleFacade` 内，是记录类型，用于承载 `Role Impact View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleImpactView` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Role Impact View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleImpactView` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleImpactView` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param activationRoots 记录组件 `activationRoots` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRoots` carries constructor data whose meaning is defined by the record contract.
     * @param roleFamily 记录组件 `roleFamily` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleFamily` carries constructor data whose meaning is defined by the record contract.
     * @param effectiveFamilyRisk 记录组件 `effectiveFamilyRisk` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `effectiveFamilyRisk` carries constructor data whose meaning is defined by the record contract.
     * @param permissionCount 记录组件 `permissionCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCount` carries constructor data whose meaning is defined by the record contract.
     * @param conflicts 记录组件 `conflicts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `conflicts` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleImpactView(
            /**
             * 字段 `roleId` 表示 `RoleImpactView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `activationRoots` 表示 `RoleImpactView` 中与 `activation Roots` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRoots` stores the `activation Roots`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRoots` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRoots`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activationRoots,
            /**
             * 字段 `roleFamily` 表示 `RoleImpactView` 中与 `role Family` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleFamily` stores the `role Family`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleFamily` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleFamily`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> roleFamily,
            /**
             * 字段 `effectiveFamilyRisk` 表示 `RoleImpactView` 中与 `effective Family Risk` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `effectiveFamilyRisk` stores the `effective Family Risk`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `effectiveFamilyRisk` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `effectiveFamilyRisk`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String effectiveFamilyRisk,
            /**
             * 字段 `permissionCount` 表示 `RoleImpactView` 中与 `permission Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCount` stores the `permission Count`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCount` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCount`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long permissionCount,
            /**
             * 字段 `conflicts` 表示 `RoleImpactView` 中与 `conflicts` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `conflicts` stores the `conflicts`-related state, dependency, configuration, or result of `RoleImpactView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `conflicts` 时应保持 `RoleImpactView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `conflicts`, preserve `RoleImpactView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> conflicts
    ) {

        /**
         * 构造器 `RoleImpactView` 用于创建并初始化 `RoleImpactView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RoleImpactView` creates and initializes `RoleImpactView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RoleImpactView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RoleImpactView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRoots 输入参数 `activationRoots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleFamily 输入参数 `roleFamily`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param effectiveFamilyRisk 输入参数 `effectiveFamilyRisk`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionCount 输入参数 `permissionCount`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param conflicts 输入参数 `conflicts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RoleImpactView {
            activationRoots = List.copyOf(activationRoots);
            roleFamily = List.copyOf(roleFamily);
            conflicts = List.copyOf(conflicts);
        }
    }

    /**
     * 类型 `RoleMutationResult` 位于 `RoleFacade` 内，是记录类型，用于承载 `Role Mutation Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleMutationResult` is a record inside `RoleFacade` and carries the responsibility, state, or contract for `Role Mutation Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleMutationResult` 作为 `RoleFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleMutationResult` as the responsibility boundary of `RoleFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param resourceId 记录组件 `resourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceId` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param propagationId 记录组件 `propagationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationId` carries constructor data whose meaning is defined by the record contract.
     * @param propagationPending 记录组件 `propagationPending` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationPending` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleMutationResult(
            /**
             * 字段 `resourceId` 表示 `RoleMutationResult` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `RoleMutationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `RoleMutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `RoleMutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceId,
            /**
             * 字段 `policyVersion` 表示 `RoleMutationResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RoleMutationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RoleMutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RoleMutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `propagationId` 表示 `RoleMutationResult` 中与 `propagation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationId` stores the `propagation Id`-related state, dependency, configuration, or result of `RoleMutationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationId` 时应保持 `RoleMutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationId`, preserve `RoleMutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String propagationId,
            /**
             * 字段 `propagationPending` 表示 `RoleMutationResult` 中与 `propagation Pending` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationPending` stores the `propagation Pending`-related state, dependency, configuration, or result of `RoleMutationResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationPending` 时应保持 `RoleMutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationPending`, preserve `RoleMutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean propagationPending
    ) {
    }
}
