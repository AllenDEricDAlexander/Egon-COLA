package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/rbac3/v1/sessions")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "session",
        name = "会话管理接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class SessionController {

    private final SessionManagementPort managementPort;
    private final DatabaseClock databaseClock;

    public SessionController(
            SessionManagementPort managementPort,
            DatabaseClock databaseClock) {
        this.managementPort = managementPort;
        this.databaseClock = databaseClock;
    }

    @GetMapping("/mine")
    @GatewayOperation(
            name = "rbac3-session-list-mine-v1",
            summary = "列出当前用户的会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelope<List<SessionView>> mine(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(managementPort.findByUser(
                principal.tenantId(), principal.userId()));
    }

    @DeleteMapping("/{sessionId}")
    @RequiresRbac3Permission(permission = "system:session:revoke")
    @GatewayOperation(
            name = "rbac3-session-revoke-v1",
            summary = "撤销指定租户会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelope<RevocationView> revoke(@PathVariable String sessionId) {
        boolean changed = managementPort.revoke(
                top.egon.cola.platform.rbac3.admin.tenant.TenantContext
                        .requireCurrent().effectiveTenantId(),
                sessionId,
                databaseClock.transactionNow());
        return ApiEnvelope.success(new RevocationView(true, changed));
    }

    public interface SessionManagementPort {

        List<SessionView> findByUser(String tenantId, String userId);

        boolean revoke(String tenantId, String sessionId, Instant now);
    }

    public record SessionView(
            String sessionId,
            String status,
            long sessionVersion,
            Instant authenticatedAt,
            Instant lastSeenAt,
            Instant absoluteExpiresAt
    ) {
    }

    public record RevocationView(boolean success, boolean stateChanged) {
    }
}
