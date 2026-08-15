package top.egon.cola.platform.rbac3.admin.iam.position.controller;

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
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.position.service.PositionFacade;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.List;

/** CRUD operations for RBAC-owned MANUAL positions. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/positions")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "iam-position",
        name = "IAM岗位接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class PositionController {

    private final PositionFacade facade;

    public PositionController(PositionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @RequiresRbac3Permission(permission = "system:position:read")
    @GatewayOperation(
            name = "rbac3-iam-position-list-v1",
            summary = "查询手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ApiEnvelopeVO<List<PositionVO>> list(
            @RequestParam(required = false) Long orgUnitId) {
        return ApiEnvelopeVO.success(facade.list(tenantId(), orgUnitId));
    }

    @PostMapping
    @RequiresRbac3Permission(permission = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-create-v1",
            summary = "创建手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ApiEnvelopeVO<PositionVO> create(
            @Valid @RequestBody PositionFacade.CreateCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.create(
                tenantId(), command, principal.userId()));
    }

    @PutMapping("/{positionId}")
    @RequiresRbac3Permission(permission = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-update-v1",
            summary = "更新手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ApiEnvelopeVO<PositionVO> update(
            @PathVariable Long positionId,
            @Valid @RequestBody PositionFacade.UpdateCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.update(
                tenantId(), positionId, command, principal.userId()));
    }

    @DeleteMapping("/{positionId}")
    @RequiresRbac3Permission(permission = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-delete-v1",
            summary = "停用手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ApiEnvelopeVO<Void> remove(
            @PathVariable Long positionId,
            @RequestParam long expectedVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        facade.remove(tenantId(), positionId, expectedVersion, principal.userId());
        return ApiEnvelopeVO.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
