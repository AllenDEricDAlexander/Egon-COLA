package top.egon.cola.platform.idp.admin.interfaces.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
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
import top.egon.cola.platform.idp.admin.integration.ddc.IdpRuntimePolicy;

import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

@RestController
public class OAuthTokenController {

    public static final String REFRESH_COOKIE_NAME = "EGON_IDP_REFRESH";
    private static final String REFRESH_COOKIE_PATH = "/oauth2";

    private final AuthorizationFacade authorizations;
    private final TokenFacade tokens;
    private final OAuthClientStore clients;
    private final IdpRuntimePolicy runtimePolicy;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthTokenController(
            AuthorizationFacade authorizations,
            TokenFacade tokens,
            OAuthClientStore clients,
            IdpRuntimePolicy runtimePolicy,
            Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie
    ) {
        this.authorizations = Objects.requireNonNull(
                authorizations,
                "authorizations"
        );
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clients = Objects.requireNonNull(clients, "clients");
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
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshCookie
    ) {
        String grantType = required(form.getFirst("grant_type"));
        OAuthClient client = activeClient(form.getFirst("client_id"));
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
                        refreshCookie(pair).toString()
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
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshCookie
    ) {
        OAuthClient client = activeClient(clientId);
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            try {
                tokens.revoke(refreshCookie, client.clientId());
            } catch (TokenException ignored) {
                // OAuth revocation does not reveal whether a token was valid.
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
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
                    required = false
            ) String clientId,
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshCookie,
            Principal principal
    ) {
        if (allSessions) {
            if (principal == null || principal.getName() == null) {
                throw oauth("invalid_request");
            }
            tokens.logoutAll(principal.getName());
        } else if (refreshCookie != null && !refreshCookie.isBlank()) {
            tokens.revoke(refreshCookie, activeClient(clientId).clientId());
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
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

    private ResponseCookie refreshCookie(TokenFacade.TokenPair pair) {
        Duration maxAge = Duration.between(
                clock.instant(),
                pair.refreshExpiresAt()
        );
        return cookie(pair.refreshToken(), maxAge.isNegative()
                ? Duration.ZERO
                : maxAge);
    }

    private ResponseCookie expiredRefreshCookie() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
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
