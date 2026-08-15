package top.egon.cola.platform.rbac3.admin.iam.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.service.DirectoryQueryService;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.CreateUserCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UpdateUserCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.user.service.UserCrudFacade;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.service.DirectoryCommandService;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

/** RBAC-only user membership administration. IdP owns credentials and profile data. */
@RestController
@RequestMapping("/api/rbac3/v1/iam")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "iam-user",
        name = "IAM用户成员接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class UserController {

    private final DirectoryCommandService commandPort;
    private final DirectoryQueryService queryPort;
    private final UserCrudFacade users;

    public UserController(
            DirectoryCommandService commandPort,
            DirectoryQueryService queryPort,
            UserCrudFacade users) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.users = users;
    }

    @GetMapping("/users")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(name = "rbac3-iam-user-list-v1", summary = "分页查询租户用户",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<DirectoryPageVO<UserDirectoryVO>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String positionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelopeVO.success(queryPort.findUsers(tenantId(), query, status,
                orgUnitId, positionId, page, size));
    }

    @PostMapping("/users")
    @RequiresRbac3Permission(permission = "system:user:manage")
    @GatewayOperation(name = "rbac3-iam-user-create-v1", summary = "创建RBAC用户成员",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<UserDirectoryVO> create(
            @Valid @RequestBody CreateUserCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(users.create(Long.valueOf(tenantId()), command, principal.userId()));
    }

    @GetMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(name = "rbac3-iam-user-get-v1", summary = "查询RBAC用户成员",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<UserDirectoryVO> user(@PathVariable String userId) {
        return ApiEnvelopeVO.success(queryPort.findUser(tenantId(), userId));
    }

    @PutMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:manage")
    @GatewayOperation(name = "rbac3-iam-user-update-v1", summary = "更新RBAC用户成员绑定",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<UserDirectoryVO> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(users.update(Long.valueOf(tenantId()), userId, command, principal.userId()));
    }

    @DeleteMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:manage")
    @GatewayOperation(name = "rbac3-iam-user-delete-v1", summary = "归档RBAC用户成员",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<Void> delete(
            @PathVariable Long userId,
            @RequestParam long expectedAuthVersion,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        users.delete(Long.valueOf(tenantId()), userId, expectedAuthVersion, principal.userId());
        return ApiEnvelopeVO.success(null);
    }

    @PutMapping("/users/{userId}/status")
    @RequiresRbac3Permission(permission = "system:user-status:manage")
    @GatewayOperation(name = "rbac3-iam-user-status-v1", summary = "变更RBAC用户成员状态",
            externalAccessible = true, tags = {"rbac3", "iam", "user"})
    public ApiEnvelopeVO<UserDirectoryVO> changeStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(commandPort.changeUserStatus(
                tenantId(), userId, command, principal.userId()));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
