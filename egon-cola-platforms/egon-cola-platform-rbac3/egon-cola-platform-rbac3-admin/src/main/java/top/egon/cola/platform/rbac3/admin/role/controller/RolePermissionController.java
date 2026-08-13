package top.egon.cola.platform.rbac3.admin.role.controller;

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
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.CreateRoleRequestDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.UpdateRoleRequestDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.BindPermissionsRequestDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.InheritanceRequestDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.CreateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.AssignPermissionsCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.RemovePermissionCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.UpdateRoleCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.dto.InheritanceCommandDTO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleVO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleMutationResultVO;

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
    public ApiEnvelopeVO<List<RoleVO>> roles(
            @RequestParam(required = false) String applicationId) {
        return ApiEnvelopeVO.success(facade.roles(tenantId(), applicationId));
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
    public ApiEnvelopeVO<RoleMutationResultVO> create(
            @Valid @RequestBody CreateRoleRequestDTO request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        if (request.privileged() && !principal.platformAdministrator()) {
            throw new Rbac3RuleViolation("PRIVILEGED_ROLE_MANAGEMENT_DENIED");
        }
        return ApiEnvelopeVO.success(facade.createRole(new CreateRoleCommandDTO(
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
    public ApiEnvelopeVO<RoleMutationResultVO> update(
            @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleRequestDTO request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.updateRole(new UpdateRoleCommandDTO(
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
    public ApiEnvelopeVO<RoleMutationResultVO> bindPermissions(
            @PathVariable String roleId,
            @Valid @RequestBody BindPermissionsRequestDTO request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.assignPermissions(
                new AssignPermissionsCommandDTO(
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
    public ApiEnvelopeVO<RoleMutationResultVO> unbindPermission(
            @PathVariable String roleId,
            @PathVariable String permissionId,
            @RequestParam String applicationId,
            @RequestParam @PositiveOrZero long expectedRoleVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.removePermission(
                new RemovePermissionCommandDTO(
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
    public ApiEnvelopeVO<RoleImpactVO> addInheritance(
            @PathVariable String roleId,
            @Valid @RequestBody InheritanceRequestDTO request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.addInheritance(new InheritanceCommandDTO(
                tenantId(), request.applicationId(), roleId, request.juniorRoleId(),
                request.expectedRoleVersion(), principal.userId()));
        return ApiEnvelopeVO.success(facade.impact(tenantId(), roleId));
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
    public ApiEnvelopeVO<RoleImpactVO> removeInheritance(
            @PathVariable String roleId,
            @PathVariable String juniorRoleId,
            @RequestParam String applicationId,
            @RequestParam @PositiveOrZero long expectedRoleVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.removeInheritance(new InheritanceCommandDTO(
                tenantId(), applicationId, roleId, juniorRoleId,
                expectedRoleVersion, principal.userId()));
        return ApiEnvelopeVO.success(facade.impact(tenantId(), roleId));
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
    public ApiEnvelopeVO<RoleImpactVO> impact(@PathVariable String roleId) {
        return ApiEnvelopeVO.success(facade.impact(tenantId(), roleId));
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

    
    
    
    }
