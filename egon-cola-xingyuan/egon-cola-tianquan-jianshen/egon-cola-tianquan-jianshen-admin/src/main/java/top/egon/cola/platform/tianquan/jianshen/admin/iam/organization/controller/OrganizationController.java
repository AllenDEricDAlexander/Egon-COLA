package top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller;

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
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.service.OrganizationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

import java.util.List;

/** CRUD and tree operations for RBAC-owned MANUAL organization units. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/organizations")
@Tag(name = "iam-organization", description = "IAM组织接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
public class OrganizationController {

    private final OrganizationFacade facade;

    public OrganizationController(OrganizationFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @RequiresPermission(value = "system:organization:read")
    @Operation(
            operationId = "rbac3-iam-organization-list-v1",
            summary = "查询手工组织",
            tags = {"rbac3", "iam", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<OrgUnitVO>> list(
            @RequestParam(required = false) Long parentId) {
        return ResultRecord.success(facade.list(tenantId(), parentId));
    }

    @PostMapping
    @RequiresPermission(value = "system:organization:manage")
    @Operation(
            operationId = "rbac3-iam-organization-create-v1",
            summary = "创建手工组织",
            tags = {"rbac3", "iam", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<OrgUnitVO> create(
            @Valid @RequestBody OrganizationFacade.CreateCommand command
) {
        return ResultRecord.success(facade.create(
                tenantId(), command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @PutMapping("/{orgUnitId}")
    @RequiresPermission(value = "system:organization:manage")
    @Operation(
            operationId = "rbac3-iam-organization-update-v1",
            summary = "更新或移动手工组织",
            tags = {"rbac3", "iam", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<OrgUnitVO> update(
            @PathVariable Long orgUnitId,
            @Valid @RequestBody OrganizationFacade.UpdateCommand command
) {
        return ResultRecord.success(facade.update(
                tenantId(), orgUnitId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{orgUnitId}")
    @RequiresPermission(value = "system:organization:manage")
    @Operation(
            operationId = "rbac3-iam-organization-delete-v1",
            summary = "停用手工组织",
            tags = {"rbac3", "iam", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<Void> remove(
            @PathVariable Long orgUnitId,
            @RequestParam long expectedVersion
) {
        facade.remove(tenantId(), orgUnitId, expectedVersion, new CurrentRbac3User().require().rbac3UserId());
        return ResultRecord.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
