package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;

@RestController
@RequestMapping("/api/rbac3/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "auth",
        name = "认证与激活引导接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class AuthController {

    private final BootstrapQueryService bootstrapQueryService;
    private final SessionFacade sessionFacade;
    private final DatabaseClock databaseClock;

    public AuthController(
            BootstrapQueryService bootstrapQueryService,
            SessionFacade sessionFacade,
            DatabaseClock databaseClock) {
        this.bootstrapQueryService = bootstrapQueryService;
        this.sessionFacade = sessionFacade;
        this.databaseClock = databaseClock;
    }

    @PostMapping("/logout")
    @GatewayOperation(
            name = "rbac3-auth-logout-v1",
            summary = "幂等注销当前会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ResponseEntity<ApiEnvelope<LogoutView>> logout(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        boolean changed = sessionFacade.logout(
                principal.tenantId(),
                principal.userId(),
                principal.sessionId(),
                databaseClock.transactionNow());
        return ResponseEntity.ok(ApiEnvelope.success(new LogoutView(true, changed)));
    }

    @GetMapping("/bootstrap")
    @GatewayOperation(
            name = "rbac3-auth-bootstrap-v1",
            summary = "读取当前激活角色的业务启动视图",
            externalAccessible = true,
            tags = {"rbac3", "bootstrap"})
    public ApiEnvelope<BootstrapView> bootstrap(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(bootstrapQueryService.query(
                principal.tenantId(), principal.userId(), principal.sessionId()));
    }

    public record LogoutView(boolean success, boolean stateChanged) {
    }
}
