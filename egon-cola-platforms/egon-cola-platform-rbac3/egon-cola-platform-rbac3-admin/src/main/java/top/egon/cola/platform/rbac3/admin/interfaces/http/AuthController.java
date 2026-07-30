package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;

import java.util.Map;

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

    private final AuthenticationFacade authenticationFacade;
    private final RefreshFacade refreshFacade;
    private final BootstrapQueryService bootstrapQueryService;
    private final SessionFacade sessionFacade;
    private final JwtKeyRingService keyRingService;
    private final DatabaseClock databaseClock;

    public AuthController(
            AuthenticationFacade authenticationFacade,
            RefreshFacade refreshFacade,
            BootstrapQueryService bootstrapQueryService,
            SessionFacade sessionFacade,
            JwtKeyRingService keyRingService,
            DatabaseClock databaseClock) {
        this.authenticationFacade = authenticationFacade;
        this.refreshFacade = refreshFacade;
        this.bootstrapQueryService = bootstrapQueryService;
        this.sessionFacade = sessionFacade;
        this.keyRingService = keyRingService;
        this.databaseClock = databaseClock;
    }

    @PostMapping("/login")
    @GatewayOperation(
            name = "rbac3-auth-login-v1",
            summary = "用户名密码登录并创建空激活角色会话",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ApiEnvelope<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiEnvelope.success(authenticationFacade.login(
                request, databaseClock.transactionNow()));
    }

    @PostMapping("/refresh")
    @GatewayOperation(
            name = "rbac3-auth-refresh-v1",
            summary = "单次轮换 Refresh Token",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ApiEnvelope<RefreshResult> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiEnvelope.success(refreshFacade.refresh(
                request.refreshToken(), databaseClock.transactionNow()));
    }

    @PostMapping("/logout")
    @GatewayOperation(
            name = "rbac3-auth-logout-v1",
            summary = "幂等注销当前会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ApiEnvelope<LogoutView> logout(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        boolean changed = sessionFacade.logout(
                principal.tenantId(),
                principal.userId(),
                principal.sessionId(),
                databaseClock.transactionNow());
        return ApiEnvelope.success(new LogoutView(true, changed));
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

    @GetMapping("/jwks")
    @GatewayOperation(
            name = "rbac3-auth-jwks-v1",
            summary = "读取当前可验证 JWT 公钥",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ApiEnvelope<Map<String, Object>> jwks() {
        return ApiEnvelope.success(keyRingService.publicJwks());
    }

    public record RefreshRequest(@NotBlank String refreshToken) {

        @Override
        public String toString() {
            return "RefreshRequest[refreshToken=<redacted>]";
        }
    }

    public record LogoutView(boolean success, boolean stateChanged) {
    }
}
