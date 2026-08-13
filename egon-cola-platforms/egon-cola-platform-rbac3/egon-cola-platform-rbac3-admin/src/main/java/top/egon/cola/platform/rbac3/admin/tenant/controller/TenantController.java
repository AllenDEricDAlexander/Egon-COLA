package top.egon.cola.platform.rbac3.admin.tenant.controller;

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
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectoryCommandService;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectoryQueryService;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectoryPageVO;

/**
 * 租户管理 HTTP 入口。
 * Tenant management HTTP entry point.
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
public class TenantController {
    private final DirectoryCommandService commandPort;
    private final DirectoryQueryService queryPort;

    public TenantController(
            DirectoryCommandService commandPort,
            DirectoryQueryService queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

/**
     * 方法 `tenants` 按照 `TenantController` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `TenantController`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param page 输入参数 `page`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param size 输入参数 `size`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-list-v1",
            summary = "分页查询平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelopeVO<DirectoryPageVO<TenantVO>> tenants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelopeVO.success(queryPort.findTenants(query, status, page, size));
    }

/**
     * 方法 `createTenant` 按照 `TenantController` 的职责处理输入，完成 `create Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createTenant` processes its inputs according to `TenantController`'s responsibility, performs the `create Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-create-v1",
            summary = "创建平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelopeVO<TenantVO> createTenant(
            @Valid @RequestBody CreateTenantCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(commandPort.createTenant(command, principal.userId()));
    }

/**
     * 方法 `changeTenantStatus` 按照 `TenantController` 的职责处理输入，完成 `change Tenant Status` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `changeTenantStatus` processes its inputs according to `TenantController`'s responsibility, performs the `change Tenant Status` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `changeTenantStatus` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `changeTenantStatus`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/platform/tenants/{tenantId}/status")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-status-v1",
            summary = "按版本变更平台租户状态",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelopeVO<TenantVO> changeTenantStatus(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantStatusCommandDTO command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        requireTargetTenant(tenantId);
        return ApiEnvelopeVO.success(commandPort.changeTenantStatus(
                tenantId, command, principal.userId()));
    }

/**
     * 方法 `tenant` 按照 `TenantController` 的职责处理输入，完成 `tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenant` processes its inputs according to `TenantController`'s responsibility, performs the `tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/platform/tenants/{tenantId}")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-get-v1",
            summary = "读取平台目标租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelopeVO<TenantVO> tenant(@PathVariable String tenantId) {
        requireTargetTenant(tenantId);
        return ApiEnvelopeVO.success(queryPort.findTenant(tenantId));
    }

/**
     * 方法 `requireTargetTenant` 按照 `TenantController` 的职责处理输入，完成 `require Target Tenant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireTargetTenant` processes its inputs according to `TenantController`'s responsibility, performs the `require Target Tenant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireTargetTenant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireTargetTenant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void requireTargetTenant(String tenantId) {
        if (!TenantContext.requireCurrent().effectiveTenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("TENANT_CONTEXT_INVALID");
        }
    }

}
