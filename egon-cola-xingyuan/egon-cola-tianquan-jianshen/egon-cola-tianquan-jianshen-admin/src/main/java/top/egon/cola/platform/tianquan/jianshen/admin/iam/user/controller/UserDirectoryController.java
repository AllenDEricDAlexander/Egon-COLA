package top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 租户用户目录 HTTP 入口。
 * Tenant user-directory HTTP entry point.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@Tag(name = "user-directory", description = "租户用户与目录接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
public class UserDirectoryController {
    private final DirectoryCommandService commandPort;
    private final DirectoryQueryService queryPort;

    public UserDirectoryController(
            DirectoryCommandService commandPort,
            DirectoryQueryService queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

/**
     * 方法 `users` 按照 `UserDirectoryController` 的职责处理输入，完成 `users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `users` processes its inputs according to `UserDirectoryController`'s responsibility, performs the `users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `users` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `users`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param positionId 输入参数 `positionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/users")
    @RequiresPermission(value = "system:user:read")
    @Operation(
            operationId = "rbac3-directory-user-list-v1",
            summary = "分页查询租户用户",
            tags = {"rbac3", "directory"}
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

/**
     * 方法 `user` 按照 `UserDirectoryController` 的职责处理输入，完成 `user` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `user` processes its inputs according to `UserDirectoryController`'s responsibility, performs the `user` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `user` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `user`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/users/{userId}")
    @RequiresPermission(value = "system:user:read")
    @Operation(
            operationId = "rbac3-directory-user-get-v1",
            summary = "读取租户用户详情",
            tags = {"rbac3", "directory"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> user(@PathVariable String userId) {
        return ResultRecord.success(queryPort.findUser(tenantId(), userId));
    }

/**
     * 方法 `changeUserStatus` 按照 `UserDirectoryController` 的职责处理输入，完成 `change User Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeUserStatus` processes its inputs according to `UserDirectoryController`'s responsibility, performs the `change User Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeUserStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeUserStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/users/{userId}/status")
    @RequiresPermission(value = "system:user-status:manage")
    @Operation(
            operationId = "rbac3-directory-user-status-v1",
            summary = "按授权版本变更租户用户状态",
            tags = {"rbac3", "directory"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<UserDirectoryVO> changeUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommandDTO command
) {
        return ResultRecord.success(commandPort.changeUserStatus(
                tenantId(), userId, command, new CurrentRbac3User().require().rbac3UserId()));
    }

/**
     * 方法 `tenantId` 按照 `UserDirectoryController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `UserDirectoryController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

}
