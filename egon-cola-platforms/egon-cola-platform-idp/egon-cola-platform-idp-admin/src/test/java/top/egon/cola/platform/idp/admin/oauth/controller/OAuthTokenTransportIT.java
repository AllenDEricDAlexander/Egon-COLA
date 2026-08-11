package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.oauth.controller.OAuthMetadataController;
import top.egon.cola.platform.idp.admin.oauth.controller.OAuthTokenController;
import top.egon.cola.platform.idp.admin.support.ddc.AtomicIdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.oauth.repo.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoAuthenticationFilter;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthTokenTransportIT {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private static final String CLIENT_ID = "gateway-admin-web";
    private static final String COOKIE_NAME =
            "EGON_IDP_REFRESH_gateway-admin-web";
    private static final String RESOURCE =
            "https://api.egon.internal/local/platform/gateway";

    private AuthorizationFacade authorizations;
    private TokenFacade tokens;
    private PrivateKeyJwtAuthenticator clientAuthenticator;
    private ClientCredentialsTokenService clientCredentialsTokens;
    private IdpSsoSessionStore ssoSessions;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authorizations = mock(AuthorizationFacade.class);
        tokens = mock(TokenFacade.class);
        clientAuthenticator = mock(PrivateKeyJwtAuthenticator.class);
        clientCredentialsTokens = mock(ClientCredentialsTokenService.class);
        ssoSessions = mock(IdpSsoSessionStore.class);
        OAuthClientStore clients = clientId -> CLIENT_ID.equals(clientId)
                ? Optional.of(client())
                : Optional.empty();
        OAuthTokenController controller = new OAuthTokenController(
                authorizations,
                tokens,
                clientAuthenticator,
                clientCredentialsTokens,
                clients,
                ssoSessions,
                new AtomicIdpRuntimePolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                false
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void clientCredentialsReturnsServiceAccessOnlyWithoutRefreshCookie()
            throws Exception {
        ClientAssertionAuthentication authentication =
                new ClientAssertionAuthentication(
                        "idp-service",
                        "idp-service-key-1",
                        "assertion-1",
                        NOW,
                        NOW.plusSeconds(60)
                );
        when(clientAuthenticator.authenticate(
                PrivateKeyJwtAuthenticator.ASSERTION_TYPE,
                "idp-service",
                "signed-assertion"
        )).thenReturn(authentication);
        when(clientCredentialsTokens.issue(
                authentication,
                URI.create(RESOURCE),
                "tenant-001",
                Set.of("rbac3:policy:read"),
                Duration.ofMinutes(15)
        )).thenReturn(new ServiceAccessToken(
                "service-access-value",
                "Bearer",
                NOW.plusSeconds(300),
                Set.of("rbac3:policy:read")
        ));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "idp-service")
                        .param("client_assertion_type",
                                PrivateKeyJwtAuthenticator.ASSERTION_TYPE)
                        .param("client_assertion", "signed-assertion")
                        .param("resource", RESOURCE)
                        .param("tenant_id", "tenant-001")
                        .param("scope", "rbac3:policy:read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token")
                        .value("service-access-value"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().string(not(containsString(
                        "refresh_token"
                ))));
    }

    @Test
    void authorizationCodeExchangeReturnsAccessOnlyAndHostOnlyCookie()
            throws Exception {
        AuthorizationCode code = authorizationCode();
        when(authorizations.consume(
                "one-time-code",
                "valid-verifier",
                "http://127.0.0.1:5173/oauth/callback",
                CLIENT_ID,
                RESOURCE
        )).thenReturn(code);
        when(tokens.issue(
                code,
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        )).thenReturn(tokenPair());

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", "one-time-code")
                        .param("code_verifier", "valid-verifier")
                        .param("redirect_uri",
                                "http://127.0.0.1:5173/oauth/callback")
                        .param("resource", RESOURCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-value"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                ))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(content().string(not(containsString(
                        "refresh-value"
                ))))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString(COOKIE_NAME + "=refresh-value")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("HttpOnly")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Path=/oauth2")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        not(containsString("Domain="))
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        not(containsString("Secure"))
                ));
    }

    @Test
    void refreshRotatesCookieAndNeverAcceptsBodyRefreshToken()
            throws Exception {
        when(tokens.refresh(
                "refresh-current",
                CLIENT_ID,
                RESOURCE,
                Duration.ofMinutes(15)
        )).thenReturn(tokenPair());

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(new Cookie(COOKIE_NAME, "refresh-current"))
                        .param("grant_type", "refresh_token")
                        .param("client_id", CLIENT_ID)
                        .param("resource", RESOURCE)
                        .param("refresh_token", "body-token-is-forbidden"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(new Cookie(COOKIE_NAME, "refresh-current"))
                        .param("grant_type", "refresh_token")
                        .param("client_id", CLIENT_ID)
                        .param("resource", RESOURCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-value"))
                .andExpect(content().string(not(containsString(
                        "refresh-value"
                ))));
    }

    @Test
    void newTokensUseTheCurrentDdcTtlSnapshot() throws Exception {
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        policy.apply(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                "1200",
                1L
        );
        policy.apply(
                AtomicIdpRuntimePolicy.REFRESH_TOKEN_TTL_KEY,
                "172800",
                1L
        );
        OAuthClientStore clients = clientId -> CLIENT_ID.equals(clientId)
                ? Optional.of(client())
                : Optional.empty();
        OAuthTokenController controller = new OAuthTokenController(
                authorizations,
                tokens,
                clientAuthenticator,
                clientCredentialsTokens,
                clients,
                ssoSessions,
                policy,
                Clock.fixed(NOW, ZoneOffset.UTC),
                false
        );
        MockMvc runtimeMvc = MockMvcBuilders.standaloneSetup(controller).build();
        AuthorizationCode code = authorizationCode();
        when(authorizations.consume(
                "dynamic-code",
                "valid-verifier",
                "http://127.0.0.1:5173/oauth/callback",
                CLIENT_ID,
                RESOURCE
        )).thenReturn(code);
        when(tokens.issue(
                code,
                Duration.ofMinutes(20),
                Duration.ofDays(2)
        )).thenReturn(tokenPair());

        runtimeMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", "dynamic-code")
                        .param("code_verifier", "valid-verifier")
                        .param("redirect_uri",
                                "http://127.0.0.1:5173/oauth/callback")
                        .param("resource", RESOURCE))
                .andExpect(status().isOk());

        verify(tokens).issue(
                code,
                Duration.ofMinutes(20),
                Duration.ofDays(2)
        );
    }

    @Test
    void tokenEndpointRequiresExactlyOneResourceAndRejectsAudience()
            throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", "one-time-code")
                        .param("code_verifier", "valid-verifier")
                        .param("redirect_uri",
                                "http://127.0.0.1:5173/oauth/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", "one-time-code")
                        .param("code_verifier", "valid-verifier")
                        .param("redirect_uri",
                                "http://127.0.0.1:5173/oauth/callback")
                        .param("resource", RESOURCE,
                                "https://api.egon.internal/local/platform/ddc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", "one-time-code")
                        .param("code_verifier", "valid-verifier")
                        .param("redirect_uri",
                                "http://127.0.0.1:5173/oauth/callback")
                        .param("resource", RESOURCE)
                        .param("audience", "legacy-api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void revokeAndLogoutClearRefreshCookie() throws Exception {
        Cookie cookie = new Cookie(COOKIE_NAME, "refresh-current");

        mockMvc.perform(post("/oauth2/revoke")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(cookie)
                        .param("client_id", CLIENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));
        verify(tokens).revoke("refresh-current", CLIENT_ID);

        mockMvc.perform(post("/oauth2/logout")
                        .principal(() -> "alice-sub")
                        .cookie(new Cookie(
                                IdpSsoAuthenticationFilter.COOKIE_NAME,
                                "sso-token"))
                        .param("client_id", CLIENT_ID)
                        .param("all_sessions", "true"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));
        verify(tokens).logoutAll("alice-sub");
        verify(ssoSessions).revoke("sso-token");
    }

    @Test
    void metadataPublishesExactIssuerEndpoints() throws Exception {
        Rs256TokenService tokenService = mock(Rs256TokenService.class);
        when(tokenService.jwkSet()).thenReturn(java.util.Map.of(
                "keys",
                List.of(java.util.Map.of("kid", "key-1"))
        ));
        MockMvc metadata = MockMvcBuilders.standaloneSetup(
                new OAuthMetadataController(
                        "https://idp.example.test",
                        tokenService
                )
        ).build();

        metadata.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer")
                        .value("https://idp.example.test"))
                .andExpect(jsonPath("$.token_endpoint")
                        .value("https://idp.example.test/oauth2/token"))
                .andExpect(jsonPath("$.jwks_uri")
                        .value("https://idp.example.test/oauth2/jwks"))
                .andExpect(jsonPath("$.grant_types_supported")
                        .value(org.hamcrest.Matchers.hasItem(
                                "client_credentials"
                        )))
                .andExpect(jsonPath("$.token_endpoint_auth_methods_supported")
                        .value(org.hamcrest.Matchers.hasItem(
                                "private_key_jwt"
                        )));
        metadata.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value("key-1"));
    }

    private OAuthClient client() {
        return new OAuthClient(
                CLIENT_ID,
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of("http://127.0.0.1:5173/oauth/callback"),
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
    }

    private AuthorizationCode authorizationCode() {
        return new AuthorizationCode(
                "alice-sub",
                "tenant-a",
                "rbac3-alice",
                "sso-session-1",
                CLIENT_ID,
                URI.create(RESOURCE),
                "platform-gateway-local",
                9L,
                "http://127.0.0.1:5173/oauth/callback",
                "nonce-value",
                "challenge-value",
                NOW.minusSeconds(1),
                NOW.plusSeconds(59)
        );
    }

    private TokenFacade.TokenPair tokenPair() {
        return new TokenFacade.TokenPair(
                "access-value",
                "refresh-value",
                "family-1",
                "family-1",
                NOW.plusSeconds(900),
                NOW.plus(Duration.ofDays(7))
        );
    }
}
