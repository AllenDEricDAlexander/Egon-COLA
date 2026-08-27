package top.egon.cola.platform.rbac3.admin.iam.application.controller;

import jakarta.validation.Valid;
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
import top.egon.cola.platform.rbac3.admin.authorization.grant.application.service.TenantApplicationFacade;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.AdmitApplicationAuthorizationScopeCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.ChangeApplicationAuthorizationScopeStatusCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.List;

/** Local RBAC Application authorization-scope endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam")
@Tag(name = "application-scope", description = "应用授权范围接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "application-scope"
)
public class ApplicationController {

    private final TenantApplicationFacade facade;

    public ApplicationController(TenantApplicationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/tenant-applications")
    @RequiresPermission(value = "system:application:manage")
    @Operation(
            operationId = "rbac3-application-scope-admit-v1",
            summary = "将 DDC 应用纳入租户授权范围",
            tags = {"rbac3", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<ApplicationAuthorizationScopeVO> admit(
            @Valid @RequestBody AdmitApplicationAuthorizationScopeCommand command
            ) {
        return ResultRecord.success(facade.admit(
                tenantId(), new CurrentRbac3User().require().rbac3UserId(), command));
    }

    @GetMapping("/tenant-applications")
    @RequiresPermission(value = "system:application:read")
    @Operation(
            operationId = "rbac3-application-scope-list-v1",
            summary = "查询租户应用授权范围",
            tags = {"rbac3", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<ApplicationAuthorizationScopeVO>> applications() {
        return ResultRecord.success(facade.applications(tenantId()));
    }

    @GetMapping("/tenant-applications/{applicationId}")
    @RequiresPermission(value = "system:application:read")
    @Operation(
            operationId = "rbac3-application-scope-get-v1",
            summary = "查询租户应用授权范围详情",
            tags = {"rbac3", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<ApplicationAuthorizationScopeVO> application(
            @PathVariable Long applicationId) {
        return ResultRecord.success(facade.application(tenantId(), applicationId));
    }

    @PutMapping("/tenant-applications/{applicationId}/status")
    @RequiresPermission(value = "system:application:manage")
    @Operation(
            operationId = "rbac3-application-scope-status-v1",
            summary = "变更租户应用授权范围状态",
            tags = {"rbac3", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<ApplicationAuthorizationScopeVO> changeStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ChangeApplicationAuthorizationScopeStatusCommand command
            ) {
        return ResultRecord.success(facade.changeStatus(
                tenantId(), applicationId,
                new CurrentRbac3User().require().rbac3UserId(), command));
    }

    @DeleteMapping("/tenant-applications/{applicationId}")
    @RequiresPermission(value = "system:application:manage")
    @Operation(
            operationId = "rbac3-application-scope-remove-v1",
            summary = "移除无依赖的租户应用授权范围",
            tags = {"rbac3", "application"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<Void> remove(
            @PathVariable Long applicationId,
            @RequestParam(name = "expectedVersion") long expectedVersion) {
        facade.remove(tenantId(), applicationId, expectedVersion,
                new CurrentRbac3User().require().rbac3UserId());
        return ResultRecord.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
