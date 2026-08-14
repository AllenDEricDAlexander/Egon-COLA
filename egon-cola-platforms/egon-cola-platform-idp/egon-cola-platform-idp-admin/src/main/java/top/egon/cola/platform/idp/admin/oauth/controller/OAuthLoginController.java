package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.OAuthLoginDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthCsrfVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthLoginErrorVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthLoginVO;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityException;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.core.token.UserTokenPair;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Password login endpoint that issues the IdP-owned USER AT/RT cookie pair.
 */
@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        code = "idp-oauth-login",
        name = "IdP OAuth 登录接口组")
public class OAuthLoginController {

    public static final String CSRF_COOKIE_NAME = "EGON_IDP_CSRF";
    public static final String USER_ACCESS_COOKIE = "__Host-egon_user_at";
    public static final String USER_REFRESH_COOKIE = "__Host-egon_user_rt";
    public static final String LOCAL_USER_ACCESS_COOKIE = "egon_user_at_local";
    public static final String LOCAL_USER_REFRESH_COOKIE = "egon_user_rt_local";

    private final IdentityFacade identities;
    private final TokenFacade tokens;
    private final IdpRuntimePolicy runtimePolicy;
    private final SecureRandom random;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthLoginController(
            IdentityFacade identities,
            TokenFacade tokens,
            IdpRuntimePolicy runtimePolicy,
            SecureRandom random,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie) {
        this.identities = Objects.requireNonNull(identities, "identities");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @GetMapping("/oauth2/login/csrf")
    @GatewayOperation(name = "idp-oauth-login-csrf-v1",
            summary = "获取 OAuth 登录 CSRF 挑战",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<OAuthCsrfVO> csrf() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, csrfCookie(token).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new OAuthCsrfVO(token));
    }

    @PostMapping("/oauth2/login")
    @GatewayOperation(name = "idp-oauth-login-v1",
            summary = "使用密码登录并建立 USER Cookie",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<OAuthLoginVO> login(
            @RequestBody OAuthLoginDTO request,
            @RequestHeader("X-IDP-CSRF") String csrfHeader,
            @CookieValue(name = CSRF_COOKIE_NAME) String csrfCookie,
            HttpServletRequest httpRequest) {
        requireCsrf(csrfHeader, csrfCookie);
        char[] password = required(request.password(), "password").toCharArray();
        AuthenticatedIdentity identity;
        try {
            identity = identities.authenticate(
                    request.username(), password, sourceBucket(httpRequest), clock.instant());
        } finally {
            Arrays.fill(password, '\0');
        }
        UserTokenPair pair = tokens.issue(
                identity, request.tenantId(), runtimePolicy.current().refreshTokenTtl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie(pair).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(pair).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCsrfCookie().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new OAuthLoginVO(identity.identitySub(), identity.displayName(),
                        identity.mustChangePassword()));
    }

    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<OAuthLoginErrorVO> authenticationFailed(
            IdentityException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new OAuthLoginErrorVO("INVALID_CREDENTIALS",
                        "username or password is invalid"));
    }

    private void requireCsrf(String header, String cookie) {
        byte[] left = Objects.requireNonNull(header, "csrfHeader")
                .getBytes(StandardCharsets.UTF_8);
        byte[] right = Objects.requireNonNull(cookie, "csrfCookie")
                .getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(left, right)) {
            throw new IllegalArgumentException("invalid CSRF token");
        }
    }

    private String sourceBucket(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank()
                ? "browser" : "browser-" + remote.replaceAll("[^A-Za-z0-9._~-]", "-");
    }

    private ResponseCookie accessCookie(UserTokenPair pair) {
        return cookie(accessCookieName(), pair.accessToken(), true,
                Duration.between(clock.instant(), pair.accessExpiresAt()));
    }

    private ResponseCookie refreshCookie(UserTokenPair pair) {
        return cookie(refreshCookieName(), pair.refreshToken(), true,
                Duration.between(clock.instant(), pair.refreshExpiresAt()));
    }

    private ResponseCookie csrfCookie(String value) {
        return cookie(CSRF_COOKIE_NAME, value, false, Duration.ofMinutes(10));
    }

    private ResponseCookie expiredCsrfCookie() {
        return cookie(CSRF_COOKIE_NAME, "", false, Duration.ZERO);
    }

    static String accessCookieName(boolean secure) {
        return secure ? USER_ACCESS_COOKIE : LOCAL_USER_ACCESS_COOKIE;
    }

    static String refreshCookieName(boolean secure) {
        return secure ? USER_REFRESH_COOKIE : LOCAL_USER_REFRESH_COOKIE;
    }

    private String accessCookieName() {
        return accessCookieName(secureCookie);
    }

    private String refreshCookieName() {
        return refreshCookieName(secureCookie);
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
