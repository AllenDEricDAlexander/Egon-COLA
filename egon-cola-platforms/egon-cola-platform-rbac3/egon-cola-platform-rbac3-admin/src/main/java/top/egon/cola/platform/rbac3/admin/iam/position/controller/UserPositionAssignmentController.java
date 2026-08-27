package top.egon.cola.platform.rbac3.admin.iam.position.controller;

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
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;import io.swagger.v3.oas.annotations.Operation;import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.platform.rbac3.admin.iam.position.service.UserPositionAssignmentService;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.List;

/** User position membership endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/users/{userId}/positions")
@Tag(name = "iam-user-position", description = "用户岗位任职接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam-user-position"
)
public class UserPositionAssignmentController {

    private final UserPositionAssignmentService service;

    public UserPositionAssignmentController(UserPositionAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(value = "system:user-position:read")
    @Operation(
            operationId = "rbac3-iam-user-position-list-v1",
            summary = "查询用户岗位任职",
            tags = {"rbac3", "iam", "user", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<UserPositionAssignmentService.AssignmentView>> list(
            @PathVariable Long userId) {
        return ResultRecord.success(service.list(tenantId(), userId));
    }

    @PostMapping
    @RequiresPermission(value = "system:user-position:manage")
    @Operation(
            operationId = "rbac3-iam-user-position-create-v1",
            summary = "新增用户岗位任职",
            tags = {"rbac3", "iam", "user", "position"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserPositionAssignmentService.AssignmentView> assign(
            @PathVariable Long userId,
            @Valid @RequestBody UserPositionAssignmentService.AssignCommand command
) {
        return ResultRecord.success(service.assign(
                tenantId(), userId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{assignmentId}")
    @RequiresPermission(value = "system:user-position:manage")
    @Operation(
            operationId = "rbac3-iam-user-position-revoke-v1",
            summary = "撤销用户岗位任职",
            tags = {"rbac3", "iam", "user", "position"}
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
