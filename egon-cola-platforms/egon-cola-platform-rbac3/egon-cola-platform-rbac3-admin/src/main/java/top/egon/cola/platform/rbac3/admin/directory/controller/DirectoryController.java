package top.egon.cola.platform.rbac3.admin.directory.controller;

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
 * 组织、岗位与目录快照 HTTP 入口。
 * Organization, position, and directory-snapshot HTTP entry point.
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
public class DirectoryController {
    private final DirectoryCommandService commandPort;
    private final DirectoryQueryService queryPort;

    public DirectoryController(
            DirectoryCommandService commandPort,
            DirectoryQueryService queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

/**
     * 方法 `orgUnits` 按照 `DirectoryController` 的职责处理输入，完成 `org Units` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `orgUnits` processes its inputs according to `DirectoryController`'s responsibility, performs the `org Units` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `orgUnits` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `orgUnits`, then continue the business flow using its result, exception, or side effect.
     *
     * @param parentId 输入参数 `parentId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/org-units")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-org-unit-list-v1",
            summary = "查询组织单元",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<List<OrgUnitVO>> orgUnits(
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiEnvelopeVO.success(queryPort.findOrgUnits(
                tenantId(), parentId, type, status));
    }

/**
     * 方法 `positions` 按照 `DirectoryController` 的职责处理输入，完成 `positions` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positions` processes its inputs according to `DirectoryController`'s responsibility, performs the `positions` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positions` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positions`, then continue the business flow using its result, exception, or side effect.
     *
     * @param orgUnitId 输入参数 `orgUnitId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/positions")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-position-list-v1",
            summary = "查询岗位",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<List<PositionVO>> positions(
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String status) {
        return ApiEnvelopeVO.success(queryPort.findPositions(
                tenantId(), orgUnitId, status));
    }

/**
     * 方法 `submit` 按照 `DirectoryController` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `DirectoryController`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/internal/directory-snapshots")
    @RequiresRbac3Permission(permission = "system:directory:sync")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-submit-v1",
            summary = "提交单调递增的目录快照",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<DirectorySyncVO> submit(
            @Valid @RequestBody DirectorySnapshotCommandDTO command) {
        return ApiEnvelopeVO.success(commandPort.submit(
                TenantContext.requireCurrent().effectiveTenantId(), command));
    }

/**
     * 方法 `snapshot` 按照 `DirectoryController` 的职责处理输入，完成 `snapshot` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `snapshot` processes its inputs according to `DirectoryController`'s responsibility, performs the `snapshot` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `snapshot` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `snapshot`, then continue the business flow using its result, exception, or side effect.
     *
     * @param snapshotId 输入参数 `snapshotId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/directory-snapshots/{snapshotId}")
    @RequiresRbac3Permission(permission = "system:directory-snapshot:read")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-get-v1",
            summary = "读取不可变目录快照回执",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelopeVO<DirectorySnapshotVO> snapshot(
            @PathVariable String snapshotId) {
        return ApiEnvelopeVO.success(queryPort.findSnapshot(tenantId(), snapshotId));
    }

/**
     * 方法 `tenantId` 按照 `DirectoryController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `DirectoryController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
