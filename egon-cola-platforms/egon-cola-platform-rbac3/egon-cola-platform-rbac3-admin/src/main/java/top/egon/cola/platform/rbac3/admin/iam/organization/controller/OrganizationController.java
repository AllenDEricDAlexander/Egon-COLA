package top.egon.cola.platform.rbac3.admin.iam.organization.controller;

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
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.service.OrganizationFacade;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** CRUD and tree operations for RBAC-owned MANUAL organization units. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/organizations")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "iam-organization",
        name = "IAM组织接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class OrganizationController {

    private final OrganizationFacade facade;

    public OrganizationController(OrganizationFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @RequiresRbac3Permission(permission = "system:organization:read")
    @GatewayOperation(
            name = "rbac3-iam-organization-list-v1",
            summary = "查询手工组织",
            externalAccessible = true,
            tags = {"rbac3", "iam", "organization"})
    public ApiEnvelopeVO<List<OrgUnitVO>> list(
            @RequestParam(required = false) Long parentId) {
        return ApiEnvelopeVO.success(facade.list(tenantId(), parentId));
    }

    @PostMapping
    @RequiresRbac3Permission(permission = "system:organization:manage")
    @GatewayOperation(
            name = "rbac3-iam-organization-create-v1",
            summary = "创建手工组织",
            externalAccessible = true,
            tags = {"rbac3", "iam", "organization"})
    public ApiEnvelopeVO<OrgUnitVO> create(
            @Valid @RequestBody OrganizationFacade.CreateCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.create(
                tenantId(), command, principal.userId()));
    }

    @PutMapping("/{orgUnitId}")
    @RequiresRbac3Permission(permission = "system:organization:manage")
    @GatewayOperation(
            name = "rbac3-iam-organization-update-v1",
            summary = "更新或移动手工组织",
            externalAccessible = true,
            tags = {"rbac3", "iam", "organization"})
    public ApiEnvelopeVO<OrgUnitVO> update(
            @PathVariable Long orgUnitId,
            @Valid @RequestBody OrganizationFacade.UpdateCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.update(
                tenantId(), orgUnitId, command, principal.userId()));
    }

    @DeleteMapping("/{orgUnitId}")
    @RequiresRbac3Permission(permission = "system:organization:manage")
    @GatewayOperation(
            name = "rbac3-iam-organization-delete-v1",
            summary = "停用手工组织",
            externalAccessible = true,
            tags = {"rbac3", "iam", "organization"})
    public ApiEnvelopeVO<Void> remove(
            @PathVariable Long orgUnitId,
            @RequestParam long expectedVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.remove(tenantId(), orgUnitId, expectedVersion, principal.userId());
        return ApiEnvelopeVO.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
