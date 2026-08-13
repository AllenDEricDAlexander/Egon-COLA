package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;

/**
 * 类型 `RolePermissionController` 位于当前包内，是类型，用于承载 `Role Permission Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RolePermissionController` is a type in its package and carries the responsibility, state, or contract for `Role Permission Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `RolePermissionController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `RolePermissionController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/roles")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-permission",
        name = "角色与权限接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class RolePermissionController {

    /**
     * 字段 `facade` 表示 `RolePermissionController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `RoleFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `RolePermissionController` (declared type `RoleFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `RolePermissionController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `RolePermissionController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleFacade facade;
    /**
     * 字段 `databaseClock` 表示 `RolePermissionController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `RolePermissionController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `RolePermissionController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `RolePermissionController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `RolePermissionController` 用于创建并初始化 `RolePermissionController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RolePermissionController` creates and initializes `RolePermissionController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RolePermissionController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RolePermissionController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RolePermissionController(RoleFacade facade, DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `roles` 按照 `RolePermissionController` 的职责处理输入，完成 `roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roles` processes its inputs according to `RolePermissionController`'s responsibility, performs the `roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping
    @RequiresRbac3Permission(permission = "system:role:read")
    @GatewayOperation(
            name = "rbac3-role-list-v1",
            summary = "查询租户角色",
            externalAccessible = true,
            tags = {"rbac3", "role"})
    public ApiEnvelope<List<RoleFacade.RoleView>> roles(
            @RequestParam(required = false) String applicationId) {
        return ApiEnvelope.success(facade.roles(tenantId(), applicationId));
    }

    /**
     * 方法 `create` 按照 `RolePermissionController` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `RolePermissionController`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping
    @RequiresRbac3Permission(permission = "system:role:create")
    @GatewayOperation(
            name = "rbac3-role-create-v1",
            summary = "创建应用角色",
            externalAccessible = true,
            tags = {"rbac3", "role"})
    public ApiEnvelope<RoleFacade.RoleMutationResult> create(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        if (request.privileged() && !principal.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        return ApiEnvelope.success(facade.createRole(new RoleFacade.CreateRoleCommand(
                tenantId(),
                request.applicationId(),
                request.roleCode(),
                request.roleName(),
                request.roleType(),
                request.riskLevel(),
                request.privileged(),
                request.landingRouteId(),
                request.landingPriority(),
                request.maximumAssignmentDays(),
                principal.userId()),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `update` 按照 `RolePermissionController` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `RolePermissionController`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/{roleId}")
    @RequiresRbac3Permission(permission = "system:role:update")
    @GatewayOperation(
            name = "rbac3-role-update-v1",
            summary = "更新角色可变属性",
            externalAccessible = true,
            tags = {"rbac3", "role"})
    public ApiEnvelope<RoleFacade.RoleMutationResult> update(
            @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.updateRole(new RoleFacade.UpdateRoleCommand(
                tenantId(),
                roleId,
                request.roleName(),
                request.status(),
                request.landingRouteId(),
                request.landingPriority(),
                request.maximumAssignmentDays(),
                request.expectedRoleVersion(),
                principal.userId()),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `bindPermissions` 按照 `RolePermissionController` 的职责处理输入，完成 `bind Permissions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bindPermissions` processes its inputs according to `RolePermissionController`'s responsibility, performs the `bind Permissions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bindPermissions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bindPermissions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/{roleId}/permissions")
    @RequiresRbac3Permission(permission = "system:role-permission:manage")
    @GatewayOperation(
            name = "rbac3-role-permission-bind-v1",
            summary = "原子绑定角色权限集合",
            externalAccessible = true,
            tags = {"rbac3", "role", "permission"})
    public ApiEnvelope<RoleFacade.RoleMutationResult> bindPermissions(
            @PathVariable String roleId,
            @Valid @RequestBody BindPermissionsRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.assignPermissions(
                new RoleFacade.AssignPermissionsCommand(
                        tenantId(),
                        request.applicationId(),
                        roleId,
                        request.permissionIds(),
                        request.validFrom(),
                        request.validTo(),
                        request.expectedRoleVersion(),
                        principal.userId()),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `unbindPermission` 按照 `RolePermissionController` 的职责处理输入，完成 `unbind Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unbindPermission` processes its inputs according to `RolePermissionController`'s responsibility, performs the `unbind Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unbindPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unbindPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @RequiresRbac3Permission(permission = "system:role-permission:manage")
    @GatewayOperation(
            name = "rbac3-role-permission-unbind-v1",
            summary = "解除角色权限绑定",
            externalAccessible = true,
            tags = {"rbac3", "role", "permission"})
    public ApiEnvelope<RoleFacade.RoleMutationResult> unbindPermission(
            @PathVariable String roleId,
            @PathVariable String permissionId,
            @RequestParam String applicationId,
            @RequestParam @PositiveOrZero long expectedRoleVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.removePermission(
                new RoleFacade.RemovePermissionCommand(
                        tenantId(),
                        applicationId,
                        roleId,
                        permissionId,
                        expectedRoleVersion,
                        principal.userId()),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `addInheritance` 按照 `RolePermissionController` 的职责处理输入，完成 `add Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `addInheritance` processes its inputs according to `RolePermissionController`'s responsibility, performs the `add Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `addInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `addInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/{roleId}/inheritances")
    @RequiresRbac3Permission(permission = "system:role-inheritance:manage")
    @GatewayOperation(
            name = "rbac3-role-inheritance-add-v1",
            summary = "新增角色继承边并重建闭包",
            externalAccessible = true,
            tags = {"rbac3", "role", "inheritance"})
    public ApiEnvelope<RoleFacade.RoleImpactView> addInheritance(
            @PathVariable String roleId,
            @Valid @RequestBody InheritanceRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.addInheritance(new RoleFacade.InheritanceCommand(
                tenantId(), request.applicationId(), roleId, request.juniorRoleId(),
                request.expectedRoleVersion(), principal.userId()));
        return ApiEnvelope.success(facade.impact(tenantId(), roleId));
    }

    /**
     * 方法 `removeInheritance` 按照 `RolePermissionController` 的职责处理输入，完成 `remove Inheritance` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removeInheritance` processes its inputs according to `RolePermissionController`'s responsibility, performs the `remove Inheritance` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removeInheritance` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removeInheritance`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param juniorRoleId 输入参数 `juniorRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @DeleteMapping("/{roleId}/inheritances/{juniorRoleId}")
    @RequiresRbac3Permission(permission = "system:role-inheritance:manage")
    @GatewayOperation(
            name = "rbac3-role-inheritance-remove-v1",
            summary = "删除角色继承边并重建闭包",
            externalAccessible = true,
            tags = {"rbac3", "role", "inheritance"})
    public ApiEnvelope<RoleFacade.RoleImpactView> removeInheritance(
            @PathVariable String roleId,
            @PathVariable String juniorRoleId,
            @RequestParam String applicationId,
            @RequestParam @PositiveOrZero long expectedRoleVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.removeInheritance(new RoleFacade.InheritanceCommand(
                tenantId(), applicationId, roleId, juniorRoleId,
                expectedRoleVersion, principal.userId()));
        return ApiEnvelope.success(facade.impact(tenantId(), roleId));
    }

    /**
     * 方法 `impact` 按照 `RolePermissionController` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `impact` processes its inputs according to `RolePermissionController`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/{roleId}/impact-analysis")
    @RequiresRbac3Permission(permission = "system:role:read")
    @GatewayOperation(
            name = "rbac3-role-impact-v1",
            summary = "分析角色族与权限扩张影响",
            externalAccessible = true,
            tags = {"rbac3", "role", "impact"})
    public ApiEnvelope<RoleFacade.RoleImpactView> impact(@PathVariable String roleId) {
        return ApiEnvelope.success(facade.impact(tenantId(), roleId));
    }

    /**
     * 方法 `tenantId` 按照 `RolePermissionController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `RolePermissionController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    /**
     * 类型 `CreateRoleRequest` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Create Role Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CreateRoleRequest` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Create Role Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CreateRoleRequest` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CreateRoleRequest` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleCode 记录组件 `roleCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleCode` carries constructor data whose meaning is defined by the record contract.
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param roleType 记录组件 `roleType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleType` carries constructor data whose meaning is defined by the record contract.
     * @param riskLevel 记录组件 `riskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `riskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param privileged 记录组件 `privileged` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privileged` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     */
    public record CreateRoleRequest(
            /**
             * 字段 `applicationId` 表示 `CreateRoleRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `roleCode` 表示 `CreateRoleRequest` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleCode,
            /**
             * 字段 `roleName` 表示 `CreateRoleRequest` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleName,
            /**
             * 字段 `roleType` 表示 `CreateRoleRequest` 中与 `role Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleType` stores the `role Type`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleType` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleType`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleType,
            /**
             * 字段 `riskLevel` 表示 `CreateRoleRequest` 中与 `risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `riskLevel` stores the `risk Level`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `riskLevel` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `riskLevel`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String riskLevel,
            /**
             * 字段 `privileged` 表示 `CreateRoleRequest` 中与 `privileged` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privileged` stores the `privileged`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privileged` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privileged`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean privileged,
            /**
             * 字段 `landingRouteId` 表示 `CreateRoleRequest` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `CreateRoleRequest` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `CreateRoleRequest` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `CreateRoleRequest` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `CreateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `CreateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays) {
    }

    /**
     * 类型 `UpdateRoleRequest` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Update Role Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UpdateRoleRequest` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Update Role Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UpdateRoleRequest` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UpdateRoleRequest` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleName 记录组件 `roleName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param landingRouteId 记录组件 `landingRouteId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingRouteId` carries constructor data whose meaning is defined by the record contract.
     * @param landingPriority 记录组件 `landingPriority` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `landingPriority` carries constructor data whose meaning is defined by the record contract.
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record UpdateRoleRequest(
            /**
             * 字段 `roleName` 表示 `UpdateRoleRequest` 中与 `role Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleName` stores the `role Name`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleName` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleName`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleName,
            /**
             * 字段 `status` 表示 `UpdateRoleRequest` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String status,
            /**
             * 字段 `landingRouteId` 表示 `UpdateRoleRequest` 中与 `landing Route Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingRouteId` stores the `landing Route Id`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingRouteId` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingRouteId`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String landingRouteId,
            /**
             * 字段 `landingPriority` 表示 `UpdateRoleRequest` 中与 `landing Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `landingPriority` stores the `landing Priority`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `landingPriority` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `landingPriority`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero int landingPriority,
            /**
             * 字段 `maximumAssignmentDays` 表示 `UpdateRoleRequest` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `expectedRoleVersion` 表示 `UpdateRoleRequest` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `UpdateRoleRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `UpdateRoleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `UpdateRoleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedRoleVersion) {
    }

    /**
     * 类型 `BindPermissionsRequest` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Bind Permissions Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BindPermissionsRequest` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Bind Permissions Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BindPermissionsRequest` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BindPermissionsRequest` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionIds 记录组件 `permissionIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record BindPermissionsRequest(
            /**
             * 字段 `applicationId` 表示 `BindPermissionsRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `BindPermissionsRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `BindPermissionsRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `BindPermissionsRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `permissionIds` 表示 `BindPermissionsRequest` 中与 `permission Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionIds` stores the `permission Ids`-related state, dependency, configuration, or result of `BindPermissionsRequest` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionIds` 时应保持 `BindPermissionsRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionIds`, preserve `BindPermissionsRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> permissionIds,
            /**
             * 字段 `validFrom` 表示 `BindPermissionsRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `BindPermissionsRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `BindPermissionsRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `BindPermissionsRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `BindPermissionsRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `BindPermissionsRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `BindPermissionsRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `BindPermissionsRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedRoleVersion` 表示 `BindPermissionsRequest` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `BindPermissionsRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `BindPermissionsRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `BindPermissionsRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedRoleVersion) {
    }

    /**
     * 类型 `InheritanceRequest` 位于 `RolePermissionController` 内，是记录类型，用于承载 `Inheritance Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `InheritanceRequest` is a record inside `RolePermissionController` and carries the responsibility, state, or contract for `Inheritance Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `InheritanceRequest` 作为 `RolePermissionController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `InheritanceRequest` as the responsibility boundary of `RolePermissionController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param juniorRoleId 记录组件 `juniorRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `juniorRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record InheritanceRequest(
            /**
             * 字段 `applicationId` 表示 `InheritanceRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `InheritanceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `InheritanceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `InheritanceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `juniorRoleId` 表示 `InheritanceRequest` 中与 `junior Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `juniorRoleId` stores the `junior Role Id`-related state, dependency, configuration, or result of `InheritanceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `juniorRoleId` 时应保持 `InheritanceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `juniorRoleId`, preserve `InheritanceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String juniorRoleId,
            /**
             * 字段 `expectedRoleVersion` 表示 `InheritanceRequest` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `InheritanceRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `InheritanceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `InheritanceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedRoleVersion) {
    }
}
