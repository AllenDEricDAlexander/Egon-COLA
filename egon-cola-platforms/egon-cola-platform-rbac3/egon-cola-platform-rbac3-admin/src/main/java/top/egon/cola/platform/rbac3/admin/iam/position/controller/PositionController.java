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
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.position.service.PositionFacade;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.component.common.core.pojo.ResultRecord;

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
public class PositionController {

    private final PositionFacade facade;

    public PositionController(PositionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @RequiresPermission(value = "system:position:read")
    @GatewayOperation(
            name = "rbac3-iam-position-list-v1",
            summary = "查询手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ResultRecord<List<PositionVO>> list(
            @RequestParam(required = false) Long orgUnitId) {
        return ResultRecord.success(facade.list(tenantId(), orgUnitId));
    }

    @PostMapping
    @RequiresPermission(value = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-create-v1",
            summary = "创建手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ResultRecord<PositionVO> create(
            @Valid @RequestBody PositionFacade.CreateCommand command
) {
        return ResultRecord.success(facade.create(
                tenantId(), command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @PutMapping("/{positionId}")
    @RequiresPermission(value = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-update-v1",
            summary = "更新手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ResultRecord<PositionVO> update(
            @PathVariable Long positionId,
            @Valid @RequestBody PositionFacade.UpdateCommand command
) {
        return ResultRecord.success(facade.update(
                tenantId(), positionId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{positionId}")
    @RequiresPermission(value = "system:position:manage")
    @GatewayOperation(
            name = "rbac3-iam-position-delete-v1",
            summary = "停用手工岗位",
            externalAccessible = true,
            tags = {"rbac3", "iam", "position"})
    public ResultRecord<Void> remove(
            @PathVariable Long positionId,
            @RequestParam long expectedVersion
) {
        facade.remove(tenantId(), positionId, expectedVersion, new CurrentRbac3User().require().rbac3UserId());
        return ResultRecord.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
