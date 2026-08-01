package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
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
import top.egon.cola.platform.rbac3.admin.auth.application.StepUpFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
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

    static final String REFRESH_COOKIE_NAME = "rbac3_refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/rbac3/v1/auth";

    private final AuthenticationFacade authenticationFacade;
    private final RefreshFacade refreshFacade;
    private final BootstrapQueryService bootstrapQueryService;
    private final SessionFacade sessionFacade;
    private final JwtKeyRingService keyRingService;
    private final StepUpFacade stepUpFacade;
    private final DatabaseClock databaseClock;

    public AuthController(
            AuthenticationFacade authenticationFacade,
            RefreshFacade refreshFacade,
            BootstrapQueryService bootstrapQueryService,
            SessionFacade sessionFacade,
            JwtKeyRingService keyRingService,
            StepUpFacade stepUpFacade,
            DatabaseClock databaseClock) {
        this.authenticationFacade = authenticationFacade;
        this.refreshFacade = refreshFacade;
        this.bootstrapQueryService = bootstrapQueryService;
        this.sessionFacade = sessionFacade;
        this.keyRingService = keyRingService;
        this.stepUpFacade = stepUpFacade;
        this.databaseClock = databaseClock;
    }

    @PostMapping("/login")
    @GatewayOperation(
            name = "rbac3-auth-login-v1",
            summary = "用户名密码登录并创建空激活角色会话",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ResponseEntity<ApiEnvelope<LoginResult>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResult result = authenticationFacade.login(
                request, databaseClock.transactionNow());
        return withRefreshCookie(
                ApiEnvelope.success(result), result.refreshToken(), result.refreshExpiresIn());
    }

    @PostMapping("/refresh")
    @GatewayOperation(
            name = "rbac3-auth-refresh-v1",
            summary = "单次轮换 Refresh Token",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ResponseEntity<ApiEnvelope<RefreshResult>> refresh(
            @Valid @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false)
            String refreshCookie) {
        String token = selectRefreshToken(request, refreshCookie);
        RefreshResult result = refreshFacade.refresh(
                token, databaseClock.transactionNow());
        return withRefreshCookie(
                ApiEnvelope.success(result), result.refreshToken(), result.refreshExpiresIn());
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
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiEnvelope.success(new LogoutView(true, changed)));
    }

    @PostMapping("/step-up")
    @GatewayOperation(
            name = "rbac3-auth-step-up-v1",
            summary = "强认证当前会话但不授予角色",
            externalAccessible = true,
            tags = {"rbac3", "authentication"})
    public ApiEnvelope<StepUpFacade.StepUpResult> stepUp(
            @Valid @RequestBody StepUpRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(stepUpFacade.stepUp(
                principal.tenantId(), principal.userId(), principal.sessionId(),
                request.method(), request.credential(),
                databaseClock.transactionNow()));
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
    public Map<String, Object> jwks() {
        return keyRingService.publicJwks();
    }

    private String selectRefreshToken(
            RefreshRequest request,
            String refreshCookie) {
        String bodyToken = request == null ? null : normalized(request.refreshToken());
        String cookieToken = normalized(refreshCookie);
        if ((bodyToken == null) == (cookieToken == null)) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        return bodyToken == null ? cookieToken : bodyToken;
    }

    private <T> ResponseEntity<ApiEnvelope<T>> withRefreshCookie(
            ApiEnvelope<T> body,
            String refreshToken,
            long maxAgeSeconds) {
        if (refreshToken == null) {
            return ResponseEntity.ok(body);
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RefreshRequest(@NotBlank String refreshToken) {

        @Override
        public String toString() {
            return "RefreshRequest[refreshToken=<redacted>]";
        }
    }

    public record StepUpRequest(
            @NotBlank String method,
            @NotBlank String credential
    ) {

        @Override
        public String toString() {
            return "StepUpRequest[method=" + method + ", credential=<redacted>]";
        }
    }

    public record LogoutView(boolean success, boolean stateChanged) {
    }
}
