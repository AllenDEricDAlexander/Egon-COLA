package top.egon.cola.platform.rbac3.admin.iam.application.controller;

import jakarta.validation.Valid;
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
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.AdmitApplicationAuthorizationScopeCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.ChangeApplicationAuthorizationScopeStatusCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.iam.application.service.TenantApplicationFacade;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** Local RBAC Application authorization-scope endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "application-scope",
        name = "应用授权范围接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class ApplicationController {

    private final TenantApplicationFacade facade;

    public ApplicationController(TenantApplicationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/tenant-applications")
    @RequiresRbac3Permission(permission = "system:application:manage")
    @GatewayOperation(
            name = "rbac3-application-scope-admit-v1",
            summary = "将 DDC 应用纳入租户授权范围",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<ApplicationAuthorizationScopeVO> admit(
            @Valid @RequestBody AdmitApplicationAuthorizationScopeCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.admit(
                tenantId(), principal.userId(), command));
    }

    @GetMapping("/tenant-applications")
    @RequiresRbac3Permission(permission = "system:application:read")
    @GatewayOperation(
            name = "rbac3-application-scope-list-v1",
            summary = "查询租户应用授权范围",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<List<ApplicationAuthorizationScopeVO>> applications() {
        return ApiEnvelopeVO.success(facade.applications(tenantId()));
    }

    @GetMapping("/tenant-applications/{applicationId}")
    @RequiresRbac3Permission(permission = "system:application:read")
    @GatewayOperation(
            name = "rbac3-application-scope-get-v1",
            summary = "查询租户应用授权范围详情",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<ApplicationAuthorizationScopeVO> application(
            @PathVariable Long applicationId) {
        return ApiEnvelopeVO.success(facade.application(tenantId(), applicationId));
    }

    @PutMapping("/tenant-applications/{applicationId}/status")
    @RequiresRbac3Permission(permission = "system:application:manage")
    @GatewayOperation(
            name = "rbac3-application-scope-status-v1",
            summary = "变更租户应用授权范围状态",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<ApplicationAuthorizationScopeVO> changeStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ChangeApplicationAuthorizationScopeStatusCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.changeStatus(
                tenantId(), applicationId, principal.userId(), command));
    }

    @DeleteMapping("/tenant-applications/{applicationId}")
    @RequiresRbac3Permission(permission = "system:application:manage")
    @GatewayOperation(
            name = "rbac3-application-scope-remove-v1",
            summary = "移除无依赖的租户应用授权范围",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<Void> remove(
            @PathVariable Long applicationId,
            @RequestParam(name = "expectedVersion") long expectedVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.remove(tenantId(), applicationId, expectedVersion, principal.userId());
        return ApiEnvelopeVO.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
