package top.egon.cola.platform.rbac3.admin.iam.user.controller;

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
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.service.DirectoryCommandService;
import top.egon.cola.platform.rbac3.admin.iam.organization.service.DirectoryQueryService;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;

/**
 * 租户用户目录 HTTP 入口。
 * Tenant user-directory HTTP entry point.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "directory",
        name = "租户用户与目录接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
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
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-list-v1",
            summary = "分页查询租户用户",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
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
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-get-v1",
            summary = "读取租户用户详情",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<UserDirectoryVO> user(@PathVariable String userId) {
        return ApiEnvelopeVO.success(queryPort.findUser(tenantId(), userId));
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
    @RequiresRbac3Permission(permission = "system:user-status:manage")
    @GatewayOperation(
            name = "rbac3-directory-user-status-v1",
            summary = "按授权版本变更租户用户状态",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<UserDirectoryVO> changeUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(commandPort.changeUserStatus(
                tenantId(), userId, command, principal.userId()));
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
