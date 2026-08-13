package top.egon.cola.platform.rbac3.admin.role.service;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.role.repository.RoleHierarchyRepository;
import top.egon.cola.platform.rbac3.admin.role.repository.RoleControlRepository;
import top.egon.cola.platform.rbac3.admin.role.repository.RoleImpactQuery;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.CreateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.AssignPermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.AssignPermissionsCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.RemovePermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.UpdateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.InheritanceCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleVO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleMutationResultVO;

/**
 * 类型 `RoleFacade` 位于当前包内，是类型，用于承载 `Role Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleFacade` is a type in its package and carries the responsibility, state, or contract for `Role Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Serializes role graph changes and rebuilds the materialized closure atomically.
 */
public final class RoleFacade implements RoleImpactQuery {

    /**
     * 字段 `hierarchyStore` 表示 `RoleFacade` 中与 `hierarchy Store` 相关的状态、依赖、配置或结果（声明类型 `RoleHierarchyRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `hierarchyStore` stores the `hierarchy Store`-related state, dependency, configuration, or result of `RoleFacade` (declared type `RoleHierarchyRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `hierarchyStore` 时应保持 `RoleFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `hierarchyStore`, preserve `RoleFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleHierarchyRepository hierarchyStore;
    /**
     * 字段 `roleControlStore` 表示 `RoleFacade` 中与 `role Control Store` 相关的状态、依赖、配置或结果（声明类型 `RoleControlRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleControlStore` stores the `role Control Store`-related state, dependency, configuration, or result of `RoleFacade` (declared type `RoleControlRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleControlStore` 时应保持 `RoleFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleControlStore`, preserve `RoleFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleControlRepository roleControlStore;
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
    public RoleFacade(RoleHierarchyRepository hierarchyStore) {
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
    public RoleFacade(RoleHierarchyRepository hierarchyStore, RoleControlRepository roleControlStore) {
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
    public RoleMutationResultVO createRole(CreateRoleCommandDTO command, Instant now) {
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
    public RoleMutationResultVO assignPermission(
            AssignPermissionCommandDTO command,
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
    public RoleMutationResultVO assignPermissions(
            AssignPermissionsCommandDTO command,
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
    public RoleMutationResultVO removePermission(
            RemovePermissionCommandDTO command,
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
    public RoleMutationResultVO updateRole(UpdateRoleCommandDTO command, Instant now) {
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
    public List<RoleVO> roles(String tenantId, String applicationId) {
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
    @Override
    public RoleImpactVO impact(String tenantId, String roleId) {
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
        addInheritance(new InheritanceCommandDTO(
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
    public void addInheritance(InheritanceCommandDTO command) {
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
    private void mutateInheritance(InheritanceCommandDTO command, boolean add) {
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
        removeInheritance(new InheritanceCommandDTO(
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
    public void removeInheritance(InheritanceCommandDTO command) {
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
    private RoleControlRepository requiredControlStore() {
        if (roleControlStore == null) {
            throw new IllegalStateException("role control store is not configured");
        }
        return roleControlStore;
    }











    }
