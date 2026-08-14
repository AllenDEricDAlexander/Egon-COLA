package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthErrorVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthTokenVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthUserTokenResultVO;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;
import top.egon.cola.platform.idp.core.token.TokenException;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.core.token.UserTokenPair;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stateless USER refresh/revoke/logout and SERVICE client-credentials transport.
 */
@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        code = "idp-oauth-token",
        name = "IdP OAuth Token 接口组")
public class OAuthTokenController {

    private final TokenFacade tokens;
    private final PrivateKeyJwtAuthenticator clientAuthenticator;
    private final ClientCredentialsTokenService clientCredentialsTokens;
    private final IdpRuntimePolicy runtimePolicy;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthTokenController(
            TokenFacade tokens,
            PrivateKeyJwtAuthenticator clientAuthenticator,
            ClientCredentialsTokenService clientCredentialsTokens,
            IdpRuntimePolicy runtimePolicy,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clientAuthenticator = Objects.requireNonNull(clientAuthenticator,
                "clientAuthenticator");
        this.clientCredentialsTokens = Objects.requireNonNull(clientCredentialsTokens,
                "clientCredentialsTokens");
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @PostMapping(value = "/oauth2/token", consumes = "application/x-www-form-urlencoded")
    @GatewayOperation(name = "idp-oauth-token-v1",
            summary = "刷新 USER Access Token 或签发 SERVICE Access Token",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<?> token(
            @RequestParam MultiValueMap<String, String> form,
            HttpServletRequest request) {
        String grantType = single(form, "grant_type");
        if ("client_credentials".equals(grantType)) {
            return clientCredentials(form, runtimePolicy.current().accessTokenTtl());
        }
        if (!"refresh_token".equals(grantType)
                || form.keySet().stream().anyMatch(key -> !"grant_type".equals(key))) {
            throw oauth("unsupported_grant_type");
        }
        String rawRefresh = cookieValue(request, refreshCookieName());
        UserTokenPair pair = tokens.refresh(required(rawRefresh));
        long expiresIn = Math.max(0L, Duration.between(clock.instant(),
                pair.accessExpiresAt()).toSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie(pair).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .body(new OAuthUserTokenResultVO("Bearer", expiresIn));
    }

    @PostMapping(value = "/oauth2/revoke", consumes = "application/x-www-form-urlencoded")
    @GatewayOperation(name = "idp-oauth-revoke-v1",
            summary = "撤销 USER Refresh Token",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<Void> revoke(HttpServletRequest request) {
        String refresh = cookieValue(request, refreshCookieName());
        if (refresh != null && !refresh.isBlank()) {
            try {
                tokens.revoke(refresh);
            } catch (TokenException ignored) {
                // Revocation is deliberately idempotent and does not disclose token state.
            }
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/oauth2/logout")
    @GatewayOperation(name = "idp-oauth-logout-v1",
            summary = "注销并删除 USER Refresh Token",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refresh = cookieValue(request, refreshCookieName());
        if (refresh != null && !refresh.isBlank()) {
            try {
                tokens.revoke(refresh);
            } catch (TokenException ignored) {
                // Logout must remain idempotent when the RT is expired or already revoked.
            }
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<OAuthTokenVO> clientCredentials(
            MultiValueMap<String, String> form,
            Duration accessTokenTtl) {
        String clientId = single(form, "client_id");
        ClientAssertionAuthentication authentication = clientAuthenticator.authenticate(
                single(form, "client_assertion_type"), clientId,
                single(form, "client_assertion"));
        ServiceAccessToken token = clientCredentialsTokens.issue(
                authentication, resource(single(form, "resource")),
                single(form, "tenant_id"), scopes(single(form, "scope")), accessTokenTtl);
        long expiresIn = Math.max(0L, Duration.between(clock.instant(),
                token.expiresAt()).toSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .body(new OAuthTokenVO(token.accessToken(), token.tokenType(), expiresIn));
    }

    private ResponseCookie accessCookie(UserTokenPair pair) {
        return cookie(OAuthLoginController.accessCookieName(secureCookie),
                pair.accessToken(), Duration.between(clock.instant(), pair.accessExpiresAt()));
    }

    private ResponseCookie expiredAccessCookie() {
        return cookie(OAuthLoginController.accessCookieName(secureCookie), "", Duration.ZERO);
    }

    private ResponseCookie expiredRefreshCookie() {
        return cookie(OAuthLoginController.refreshCookieName(secureCookie), "", Duration.ZERO);
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private String refreshCookieName() {
        return OAuthLoginController.refreshCookieName(secureCookie);
    }

    private static URI resource(String value) {
        try {
            URI uri = URI.create(required(value));
            if (!uri.isAbsolute() || uri.getFragment() != null
                    || !uri.equals(uri.normalize())) {
                throw oauth("invalid_target");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw oauth("invalid_target");
        }
    }

    private static Set<String> scopes(String value) {
        String raw = required(value);
        if (raw.contains("  ")) throw oauth("invalid_scope");
        String[] values = raw.split(" ");
        TreeSet<String> result = new TreeSet<>();
        Collections.addAll(result, values);
        if (result.isEmpty() || result.size() != values.length
                || result.stream().anyMatch(String::isBlank)) {
            throw oauth("invalid_scope");
        }
        return Collections.unmodifiableSet(result);
    }

    private static String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private static String single(MultiValueMap<String, String> form, String name) {
        List<String> values = form.get(name);
        if (values == null || values.size() != 1) throw oauth("invalid_request");
        return required(values.getFirst());
    }

    private static String required(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request");
        }
        return value;
    }

    private static OAuthException oauth(String error) {
        return new OAuthException(error, "OAuth request is invalid");
    }

    @ExceptionHandler({OAuthException.class, TokenException.class})
    public ResponseEntity<OAuthErrorVO> oauthError(RuntimeException exception) {
        String error = exception instanceof OAuthException oauthException
                ? oauthException.oauthError() : ((TokenException) exception).oauthError();
        HttpStatus status = "invalid_client".equals(error)
                ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new OAuthErrorVO(error, "OAuth request is invalid"));
    }
}
