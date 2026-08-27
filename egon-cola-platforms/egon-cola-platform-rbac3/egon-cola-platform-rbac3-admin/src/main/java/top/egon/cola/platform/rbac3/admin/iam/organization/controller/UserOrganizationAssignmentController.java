package top.egon.cola.platform.rbac3.admin.iam.organization.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.rbac3.admin.iam.organization.service.UserOrganizationAssignmentService;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.List;

/** User organization membership endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/users/{userId}/organizations")
@Tag(name = "iam-user-organization", description = "用户组织任职接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
public class UserOrganizationAssignmentController {

    private final UserOrganizationAssignmentService service;

    public UserOrganizationAssignmentController(UserOrganizationAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(value = "system:user-organization:read")
    @Operation(
            operationId = "rbac3-iam-user-organization-list-v1",
            summary = "查询用户组织任职",
            tags = {"rbac3", "iam", "user", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<UserOrganizationAssignmentService.AssignmentView>> list(
            @PathVariable Long userId) {
        return ResultRecord.success(service.list(tenantId(), userId));
    }

    @PostMapping
    @RequiresPermission(value = "system:user-organization:manage")
    @Operation(
            operationId = "rbac3-iam-user-organization-create-v1",
            summary = "新增用户组织任职",
            tags = {"rbac3", "iam", "user", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserOrganizationAssignmentService.AssignmentView> assign(
            @PathVariable Long userId,
            @Valid @RequestBody UserOrganizationAssignmentService.AssignCommand command
) {
        return ResultRecord.success(service.assign(
                tenantId(), userId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{assignmentId}")
    @RequiresPermission(value = "system:user-organization:manage")
    @Operation(
            operationId = "rbac3-iam-user-organization-revoke-v1",
            summary = "撤销用户组织任职",
            tags = {"rbac3", "iam", "user", "organization"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<Void> revoke(
            @PathVariable Long userId,
            @PathVariable Long assignmentId,
            @RequestParam long expectedVersion
) {
        service.revoke(tenantId(), userId, assignmentId, expectedVersion, new CurrentRbac3User().require().rbac3UserId());
        return ResultRecord.success(null);
    }

    private static Long tenantId() {
        return Long.valueOf(TenantContext.requireCurrent().effectiveTenantId());
    }
}
