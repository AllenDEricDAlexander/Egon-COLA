package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.OAuthLoginDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthCsrfVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthLoginErrorVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthLoginVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityException;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/** Establishes the host-only SSO cookie after password authentication. */
@RestController
public class OAuthLoginController {

    public static final String CSRF_COOKIE_NAME = "EGON_IDP_CSRF";
    private static final Duration SSO_TTL = Duration.ofHours(12);

    private final IdentityFacade identities;
    private final IdpSsoSessionStore sessions;
    private final SecureRandom random;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthLoginController(
            IdentityFacade identities,
            IdpSsoSessionStore sessions,
            SecureRandom random,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie) {
        this.identities = Objects.requireNonNull(identities, "identities");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @GetMapping("/oauth2/login/csrf")
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
                    request.username(),
                    password,
                    sourceBucket(httpRequest),
                    clock.instant()
            );
        } finally {
            Arrays.fill(password, '\0');
        }
        String ssoToken = sessions.create(identity.identitySub(), SSO_TTL);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, ssoCookie(ssoToken).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCsrfCookie().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new OAuthLoginVO(identity.identitySub(), identity.displayName(),
                        identity.mustChangePassword()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IdentityException.class)
    public ResponseEntity<OAuthLoginErrorVO> authenticationFailed(
            IdentityException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new OAuthLoginErrorVO(
                        "INVALID_CREDENTIALS",
                        "username or password is invalid"
                ));
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
                ? "browser"
                : "browser-" + remote.replaceAll("[^A-Za-z0-9._~-]", "-");
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private ResponseCookie ssoCookie(String value) {
        return cookie(IdpSsoAuthenticationFilter.COOKIE_NAME, value, true, SSO_TTL);
    }

    private ResponseCookie csrfCookie(String value) {
        return cookie(CSRF_COOKIE_NAME, value, false, Duration.ofMinutes(10));
    }

    private ResponseCookie expiredCsrfCookie() {
        return cookie(CSRF_COOKIE_NAME, "", false, Duration.ZERO);
    }

    private ResponseCookie cookie(
            String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/oauth2")
                .maxAge(maxAge)
                .build();
    }
}
