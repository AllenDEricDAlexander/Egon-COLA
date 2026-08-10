package top.egon.cola.platform.idp.admin.interfaces.http;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.token.TokenException;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;

import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OAuthTokenController {

    public static final String REFRESH_COOKIE_PREFIX = "EGON_IDP_REFRESH_";
    private static final String REFRESH_COOKIE_PATH = "/oauth2";

    private final AuthorizationFacade authorizations;
    private final TokenFacade tokens;
    private final OAuthClientStore clients;
    private final IdpSsoSessionStore ssoSessions;
    private final IdpRuntimePolicy runtimePolicy;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthTokenController(
            AuthorizationFacade authorizations,
            TokenFacade tokens,
            OAuthClientStore clients,
            IdpSsoSessionStore ssoSessions,
            IdpRuntimePolicy runtimePolicy,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie
    ) {
        this.authorizations = Objects.requireNonNull(
                authorizations,
                "authorizations"
        );
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.ssoSessions = Objects.requireNonNull(ssoSessions, "ssoSessions");
        this.runtimePolicy = Objects.requireNonNull(
                runtimePolicy,
                "runtimePolicy"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @PostMapping(
            value = "/oauth2/token",
            consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<TokenResponse> token(
            @RequestParam MultiValueMap<String, String> form,
            HttpServletRequest request
    ) {
        String grantType = required(form.getFirst("grant_type"));
        OAuthClient client = activeClient(form.getFirst("client_id"));
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        IdpRuntimePolicy.Snapshot policy = runtimePolicy.current();
        TokenFacade.TokenPair pair;
        if ("authorization_code".equals(grantType)) {
            AuthorizationCode authorizationCode = authorizations.consume(
                    required(form.getFirst("code")),
                    required(form.getFirst("code_verifier")),
                    required(form.getFirst("redirect_uri")),
                    client.clientId()
            );
            pair = tokens.issue(
                    authorizationCode,
                    policy.accessTokenTtl(),
                    policy.refreshTokenTtl()
            );
        } else if ("refresh_token".equals(grantType)) {
            if (form.containsKey("refresh_token")) {
                throw oauth("invalid_request");
            }
            pair = tokens.refresh(
                    required(refreshCookie),
                    client.clientId(),
                    policy.accessTokenTtl()
            );
        } else {
            throw oauth("unsupported_grant_type");
        }
        long expiresIn = Duration.between(
                clock.instant(),
                pair.accessExpiresAt()
        ).toSeconds();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(pair, client.clientId()).toString()
                )
                .body(new TokenResponse(
                        pair.accessToken(),
                        "Bearer",
                        Math.max(0L, expiresIn)
                ));
    }

    @PostMapping(
            value = "/oauth2/revoke",
            consumes = "application/x-www-form-urlencoded"
    )
    public ResponseEntity<Void> revoke(
            @RequestParam("client_id") String clientId,
            HttpServletRequest request
    ) {
        OAuthClient client = activeClient(clientId);
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            try {
                tokens.revoke(refreshCookie, client.clientId());
            } catch (TokenException ignored) {
                // OAuth revocation does not reveal whether a token was valid.
            }
        }
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie(client.clientId()).toString()
                )
                .build();
    }

    @PostMapping("/oauth2/logout")
    public ResponseEntity<Void> logout(
            @RequestParam(
                    name = "all_sessions",
                    defaultValue = "false"
            ) boolean allSessions,
            @RequestParam(
                    name = "client_id",
                    required = true
            ) String clientId,
            HttpServletRequest request,
            Principal principal
    ) {
        OAuthClient client = activeClient(clientId);
        String refreshCookie = cookieValue(
                request,
                refreshCookieName(client.clientId())
        );
        String ssoCookie = cookieValue(
                request,
                IdpSsoAuthenticationFilter.COOKIE_NAME
        );
        if (allSessions) {
            if (principal == null || principal.getName() == null) {
                throw oauth("invalid_request");
            }
            tokens.logoutAll(principal.getName());
        } else if (refreshCookie != null && !refreshCookie.isBlank()) {
            tokens.revoke(refreshCookie, client.clientId());
        }
        if (ssoCookie != null && !ssoCookie.isBlank()) {
            ssoSessions.revoke(ssoCookie);
        }
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredRefreshCookie(client.clientId()).toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        expiredSsoCookie().toString()
                )
                .build();
    }

    @ExceptionHandler({OAuthException.class, TokenException.class})
    public ResponseEntity<OAuthErrorResponse> oauthError(
            RuntimeException exception
    ) {
        String error = exception instanceof OAuthException oauthException
                ? oauthException.oauthError()
                : ((TokenException) exception).oauthError();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new OAuthErrorResponse(error, "OAuth request is invalid")
        );
    }

    private OAuthClient activeClient(String clientId) {
        OAuthClient client = clients.findById(required(clientId))
                .orElseThrow(() -> oauth("unauthorized_client"));
        if (client.status() != OAuthClient.Status.ACTIVE) {
            throw oauth("unauthorized_client");
        }
        return client;
    }

    private ResponseCookie refreshCookie(
            TokenFacade.TokenPair pair,
            String clientId
    ) {
        Duration maxAge = Duration.between(
                clock.instant(),
                pair.refreshExpiresAt()
        );
        return cookie(clientId, pair.refreshToken(), maxAge.isNegative()
                ? Duration.ZERO
                : maxAge);
    }

    private ResponseCookie expiredRefreshCookie(String clientId) {
        return cookie(clientId, "", Duration.ZERO);
    }

    private ResponseCookie expiredSsoCookie() {
        return ResponseCookie.from(IdpSsoAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie cookie(
            String clientId,
            String value,
            Duration maxAge
    ) {
        return ResponseCookie.from(refreshCookieName(clientId), value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public static String refreshCookieName(String clientId) {
        String value = required(clientId);
        if (!value.matches("[A-Za-z0-9_-]{1,100}")) {
            throw oauth("invalid_request");
        }
        return REFRESH_COOKIE_PREFIX + value;
    }

    private static String cookieValue(
            HttpServletRequest request,
            String name
    ) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static OAuthException oauth(String error) {
        return new OAuthException(error, "OAuth request is invalid");
    }

    private static String required(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request");
        }
        return value;
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    public record OAuthErrorResponse(
            String error,
            @JsonProperty("error_description") String errorDescription
    ) {
    }
}
