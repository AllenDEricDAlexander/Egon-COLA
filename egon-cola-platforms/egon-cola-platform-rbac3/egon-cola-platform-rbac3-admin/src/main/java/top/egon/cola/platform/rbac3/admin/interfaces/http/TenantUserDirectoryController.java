package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/rbac3/v1/directory")
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

    @PostMapping("/snapshots")
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

    @GetMapping("/users/{userId}")
    @RequiresRbac3Permission(permission = "system:user:read")
    @GatewayOperation(
            name = "rbac3-directory-user-get-v1",
            summary = "读取租户内用户目录快照",
            externalAccessible = true,
            tags = {"rbac3", "directory"})
    public ApiEnvelope<UserDirectoryView> user(@PathVariable String userId) {
        return ApiEnvelope.success(queryPort.findUser(
                TenantContext.requireCurrent().effectiveTenantId(), userId));
    }

    @FunctionalInterface
    public interface DirectoryCommandPort {

        DirectorySyncView submit(String tenantId, DirectorySnapshotCommand command);
    }

    @FunctionalInterface
    public interface DirectoryQueryPort {

        UserDirectoryView findUser(String tenantId, String userId);
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
            long directorySnapshotVersion
    ) {
    }
}
