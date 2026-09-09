package top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.service.DirectoryQueryService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.snapshot.service.DirectoryCommandService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.dto.CreateUserCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.dto.UpdateUserCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.service.UserCrudFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

/** RBAC-only user membership administration. IdP owns credentials and profile data. */
@RestController
@RequestMapping("/api/rbac3/v1/iam")
@Tag(name = "iam-user", description = "IAM用户成员接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
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
    @RequiresPermission(value = "system:user:read")
    @Operation(
            operationId = "rbac3-iam-user-list-v1",
            summary = "分页查询租户用户",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<DirectoryPageVO<UserDirectoryVO>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String positionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResultRecord.success(queryPort.findUsers(tenantId(), query, status,
                orgUnitId, positionId, page, size));
    }

    @PostMapping("/users")
    @RequiresPermission(value = "system:user:manage")
    @Operation(
            operationId = "rbac3-iam-user-create-v1",
            summary = "创建RBAC用户成员",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> create(
            @Valid @RequestBody CreateUserCommandDTO command
            ) {
        return ResultRecord.success(users.create(Long.valueOf(tenantId()), command,
                new CurrentRbac3User().require().rbac3UserId()));
    }

    @GetMapping("/users/{userId}")
    @RequiresPermission(value = "system:user:read")
    @Operation(
            operationId = "rbac3-iam-user-get-v1",
            summary = "查询RBAC用户成员",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> user(@PathVariable String userId) {
        return ResultRecord.success(queryPort.findUser(tenantId(), userId));
    }

    @PutMapping("/users/{userId}")
    @RequiresPermission(value = "system:user:manage")
    @Operation(
            operationId = "rbac3-iam-user-update-v1",
            summary = "更新RBAC用户成员绑定",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserCommandDTO command
            ) {
        return ResultRecord.success(users.update(Long.valueOf(tenantId()), userId, command,
                new CurrentRbac3User().require().rbac3UserId()));
    }

    @DeleteMapping("/users/{userId}")
    @RequiresPermission(value = "system:user:manage")
    @Operation(
            operationId = "rbac3-iam-user-delete-v1",
            summary = "归档RBAC用户成员",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<Void> delete(
            @PathVariable Long userId,
            @RequestParam long expectedAuthVersion) {
        users.delete(Long.valueOf(tenantId()), userId, expectedAuthVersion,
                new CurrentRbac3User().require().rbac3UserId());
        return ResultRecord.success(null);
    }

    @PutMapping("/users/{userId}/status")
    @RequiresPermission(value = "system:user-status:manage")
    @Operation(
            operationId = "rbac3-iam-user-status-v1",
            summary = "变更RBAC用户成员状态",
            tags = {"rbac3", "iam", "user"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> changeStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommandDTO command
            ) {
        return ResultRecord.success(commandPort.changeUserStatus(
                tenantId(), userId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
