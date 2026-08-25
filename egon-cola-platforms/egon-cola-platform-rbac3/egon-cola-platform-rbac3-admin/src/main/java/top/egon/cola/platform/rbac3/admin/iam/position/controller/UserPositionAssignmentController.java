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
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;
import top.egon.cola.platform.rbac3.admin.iam.position.service.UserPositionAssignmentService;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.component.common.core.pojo.ResultRecord;

import java.util.List;

/** User position membership endpoints. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/users/{userId}/positions")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "iam-user-position",
        name = "用户岗位任职接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class UserPositionAssignmentController {

    private final UserPositionAssignmentService service;

    public UserPositionAssignmentController(UserPositionAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(value = "system:user-position:read")
    @GatewayOperation(
            name = "rbac3-iam-user-position-list-v1",
            summary = "查询用户岗位任职",
            externalAccessible = true,
            tags = {"rbac3", "iam", "user", "position"})
    public ResultRecord<List<UserPositionAssignmentService.AssignmentView>> list(
            @PathVariable Long userId) {
        return ResultRecord.success(service.list(tenantId(), userId));
    }

    @PostMapping
    @RequiresPermission(value = "system:user-position:manage")
    @GatewayOperation(
            name = "rbac3-iam-user-position-create-v1",
            summary = "新增用户岗位任职",
            externalAccessible = true,
            tags = {"rbac3", "iam", "user", "position"})
    public ResultRecord<UserPositionAssignmentService.AssignmentView> assign(
            @PathVariable Long userId,
            @Valid @RequestBody UserPositionAssignmentService.AssignCommand command
) {
        return ResultRecord.success(service.assign(
                tenantId(), userId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/{assignmentId}")
    @RequiresPermission(value = "system:user-position:manage")
    @GatewayOperation(
            name = "rbac3-iam-user-position-revoke-v1",
            summary = "撤销用户岗位任职",
            externalAccessible = true,
            tags = {"rbac3", "iam", "user", "position"})
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
