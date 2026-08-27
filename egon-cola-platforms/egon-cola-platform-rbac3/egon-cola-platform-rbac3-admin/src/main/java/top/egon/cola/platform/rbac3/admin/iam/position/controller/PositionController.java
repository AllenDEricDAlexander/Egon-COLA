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
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.position.service.PositionFacade;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.List;

/** CRUD operations for RBAC-owned MANUAL positions. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/positions")
@Tag(name = "iam-position", description = "IAM岗位接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam-position"
)
public class PositionController {

    private final PositionFacade facade;

    public PositionController(PositionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @RequiresPermission(value = "system:position:read")
    @Operation(
            operationId = "rbac3-iam-position-list-v1",
            summary = "查询手工岗位",
            tags = {"rbac3", "iam", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<PositionVO>> list(
            @RequestParam(required = false) Long orgUnitId) {
        return ResultRecord.success(facade.list(tenantId(), orgUnitId));
    }

    @PostMapping
    @RequiresPermission(value = "system:position:manage")
    @Operation(
            operationId = "rbac3-iam-position-create-v1",
            summary = "创建手工岗位",
            tags = {"rbac3", "iam", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<PositionVO> create(
            @Valid @RequestBody PositionFacade.CreateCommand command
) {
        return ResultRecord.success(facade.create(
                tenantId(), command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @PutMapping("/{positionId}")
    @RequiresPermission(value = "system:position:manage")
    @Operation(
            operationId = "rbac3-iam-position-update-v1",
            summary = "更新手工岗位",
            tags = {"rbac3", "iam", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<PositionVO> update(
            @PathVariable Long positionId,
            @Valid @RequestBody PositionFacade.UpdateCommand command
) {
        return ResultRecord.success(facade.update(
                tenantId(), positionId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{positionId}")
    @RequiresPermission(value = "system:position:manage")
    @Operation(
            operationId = "rbac3-iam-position-delete-v1",
            summary = "停用手工岗位",
            tags = {"rbac3", "iam", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
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
