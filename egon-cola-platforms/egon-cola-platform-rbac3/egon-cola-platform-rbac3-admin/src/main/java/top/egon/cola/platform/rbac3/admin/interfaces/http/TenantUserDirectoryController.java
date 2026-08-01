package top.egon.cola.platform.rbac3.admin.interfaces.http;

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
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
public class TenantUserDirectoryController {

    private final DirectoryCommandPort commandPort;
    private final DirectoryQueryPort queryPort;

    public TenantUserDirectoryController(
            DirectoryCommandPort commandPort,
            DirectoryQueryPort queryPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
    }

    @GetMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-list-v1",
            summary = "分页查询平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<PageView<TenantView>> tenants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelope.success(queryPort.findTenants(query, status, page, size));
    }

    @PostMapping("/platform/tenants")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-create-v1",
            summary = "创建平台租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> createTenant(
            @Valid @RequestBody CreateTenantCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(commandPort.createTenant(command, principal.userId()));
    }

    @PutMapping("/platform/tenants/{tenantId}/status")
    @RequiresRbac3Permission(permission = "system:tenant:manage")
    @GatewayOperation(
            name = "rbac3-platform-tenant-status-v1",
            summary = "按版本变更平台租户状态",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> changeTenantStatus(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantStatusCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        requireTargetTenant(tenantId);
        return ApiEnvelope.success(commandPort.changeTenantStatus(
                tenantId, command, principal.userId()));
    }

    @GetMapping("/users")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-list-v1",
            summary = "分页查询租户用户",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<PageView<UserDirectoryView>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String positionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ApiEnvelope.success(queryPort.findUsers(tenantId(), query, status,
                orgUnitId, positionId, page, size));
    }

    @GetMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-get-v1",
            summary = "读取租户用户详情",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<UserDirectoryView> user(@PathVariable String userId) {
        return ApiEnvelope.success(queryPort.findUser(tenantId(), userId));
    }

    @PutMapping("/users/{userId}/status")
    @RequiresRbac3Permission(permission = "system:user-status:manage")
    @GatewayOperation(
            name = "rbac3-directory-user-status-v1",
            summary = "按授权版本变更租户用户状态",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<UserDirectoryView> changeUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusCommand command,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(commandPort.changeUserStatus(
                tenantId(), userId, command, principal.userId()));
    }

    @GetMapping("/org-units")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-org-unit-list-v1",
            summary = "查询组织单元",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<List<OrgUnitView>> orgUnits(
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiEnvelope.success(queryPort.findOrgUnits(
                tenantId(), parentId, type, status));
    }

    @GetMapping("/positions")
    @RequiresRbac3Permission(permission = "system:directory:read")
    @GatewayOperation(
            name = "rbac3-directory-position-list-v1",
            summary = "查询岗位",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<List<PositionView>> positions(
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String status) {
        return ApiEnvelope.success(queryPort.findPositions(
                tenantId(), orgUnitId, status));
    }

    @PostMapping("/internal/directory-snapshots")
    @RequiresRbac3Permission(permission = "system:directory:sync")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-submit-v1",
            summary = "提交单调递增的目录快照",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<DirectorySyncView> submit(
            @Valid @RequestBody DirectorySnapshotCommand command) {
        return ApiEnvelope.success(commandPort.submit(
                TenantContext.requireCurrent().effectiveTenantId(), command));
    }

    @GetMapping("/directory-snapshots/{snapshotId}")
    @RequiresRbac3Permission(permission = "system:directory-snapshot:read")
    @GatewayOperation(
            name = "rbac3-directory-snapshot-get-v1",
            summary = "读取不可变目录快照回执",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<DirectorySnapshotView> snapshot(
            @PathVariable String snapshotId) {
        return ApiEnvelope.success(queryPort.findSnapshot(tenantId(), snapshotId));
    }

    @GetMapping("/platform/tenants/{tenantId}")
    @RequiresRbac3Permission(permission = "system:tenant:read")
    @GatewayOperation(
            name = "rbac3-platform-tenant-get-v1",
            summary = "读取平台目标租户",
            externalAccessible = true,
            tags = {"rbac3", "tenant"})
    public ApiEnvelope<TenantView> tenant(@PathVariable String tenantId) {
        requireTargetTenant(tenantId);
        return ApiEnvelope.success(queryPort.findTenant(tenantId));
    }

    public interface DirectoryCommandPort {

        DirectorySyncView submit(String tenantId, DirectorySnapshotCommand command);

        TenantView createTenant(CreateTenantCommand command, String actorId);

        TenantView changeTenantStatus(
                String tenantId, TenantStatusCommand command, String actorId);

        UserDirectoryView changeUserStatus(
                String tenantId, String userId, UserStatusCommand command, String actorId);
    }

    public interface DirectoryQueryPort {

        UserDirectoryView findUser(String tenantId, String userId);

        TenantView findTenant(String tenantId);

        PageView<TenantView> findTenants(String query, String status, int page, int size);

        PageView<UserDirectoryView> findUsers(
                String tenantId, String query, String status, String orgUnitId,
                String positionId, int page, int size);

        List<OrgUnitView> findOrgUnits(
                String tenantId, String parentId, String type, String status);

        List<PositionView> findPositions(
                String tenantId, String orgUnitId, String status);

        DirectorySnapshotView findSnapshot(String tenantId, String snapshotId);
    }

    public record CreateTenantCommand(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull Map<String, Object> settings
    ) {
    }

    public record TenantStatusCommand(
            @NotBlank String status,
            @NotBlank String reason,
            @PositiveOrZero long expectedVersion
    ) {
    }

    public record UserStatusCommand(
            @NotBlank String status,
            @NotBlank String reason,
            @PositiveOrZero long expectedAuthVersion
    ) {
    }

    public record DirectorySnapshotCommand(
            @NotBlank String providerCode,
            @PositiveOrZero long snapshotVersion,
            @NotBlank String checksum,
            @NotNull Instant generatedAt,
            @NotNull Map<String, Object> payload
    ) {
    }

    public record DirectorySyncView(
            String snapshotId,
            String outcome,
            Map<String, Long> counts,
            long affectedUserCount
    ) {
    }

    public record UserDirectoryView(
            String userId,
            String username,
            String displayName,
            String status,
            long authVersion,
            String primaryOrgUnitId,
            String primaryPositionId,
            long directorySnapshotVersion
    ) {
    }

    public record TenantView(
            String tenantId,
            String tenantCode,
            String tenantName,
            String status,
            Map<String, Object> settings,
            long version
    ) {
    }

    public record OrgUnitView(
            String orgUnitId,
            String snapshotId,
            String type,
            String code,
            String name,
            String parentId,
            String path,
            int depth,
            String status
    ) {
    }

    public record PositionView(
            String positionId,
            String snapshotId,
            String code,
            String name,
            String orgUnitId,
            String status
    ) {
    }

    public record DirectorySnapshotView(
            String snapshotId,
            String providerCode,
            long snapshotVersion,
            String checksum,
            String status,
            Instant generatedAt,
            Instant receivedAt,
            Instant activatedAt,
            Map<String, Object> counts
    ) {
    }

    public record PageView<T>(
            List<T> items,
            int page,
            int size,
            long total
    ) {
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    private static void requireTargetTenant(String tenantId) {
        if (!TenantContext.requireCurrent().effectiveTenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("TENANT_CONTEXT_INVALID");
        }
    }
}
