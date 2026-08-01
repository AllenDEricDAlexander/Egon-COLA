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

    private final RoleFacade facade;
    private final DatabaseClock databaseClock;

    public RolePermissionController(RoleFacade facade, DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

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

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record CreateRoleRequest(
            @NotBlank String applicationId,
            @NotBlank String roleCode,
            @NotBlank String roleName,
            @NotBlank String roleType,
            @NotBlank String riskLevel,
            boolean privileged,
            String landingRouteId,
            @PositiveOrZero int landingPriority,
            Integer maximumAssignmentDays) {
    }

    public record UpdateRoleRequest(
            @NotBlank String roleName,
            @NotBlank String status,
            String landingRouteId,
            @PositiveOrZero int landingPriority,
            Integer maximumAssignmentDays,
            @PositiveOrZero long expectedRoleVersion) {
    }

    public record BindPermissionsRequest(
            @NotBlank String applicationId,
            @NotEmpty List<@NotBlank String> permissionIds,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedRoleVersion) {
    }

    public record InheritanceRequest(
            @NotBlank String applicationId,
            @NotBlank String juniorRoleId,
            @PositiveOrZero long expectedRoleVersion) {
    }
}
