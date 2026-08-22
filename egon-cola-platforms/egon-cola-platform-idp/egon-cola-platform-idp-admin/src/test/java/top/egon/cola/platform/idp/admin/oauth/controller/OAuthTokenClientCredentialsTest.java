package top.egon.cola.platform.idp.admin.oauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.oauth.service.impl.ClientSecretBasicAuthenticator;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.core.oauth.ClientSecretAuthentication;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;
import top.egon.cola.platform.idp.core.token.TokenFacade;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthTokenClientCredentialsTest {

    private static final Instant NOW = Instant.parse("2026-08-22T00:00:00Z");
    private static final URI RESOURCE = URI.create(
            "https://api.example/prod/permission/rbac3"
    );

    private final TokenFacade tokens = mock(TokenFacade.class);
    private final ClientSecretBasicAuthenticator authenticator =
            mock(ClientSecretBasicAuthenticator.class);
    private final ClientCredentialsTokenService clientTokens =
            mock(ClientCredentialsTokenService.class);
    private final IdpRuntimePolicy runtimePolicy = mock(IdpRuntimePolicy.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(runtimePolicy.current()).thenReturn(new IdpRuntimePolicy.Snapshot(
                Duration.ofMinutes(5),
                Duration.ofDays(1),
                Duration.ofMinutes(1),
                5,
                Duration.ofMinutes(5),
                4,
                Map.of()
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(new OAuthTokenController(
                tokens,
                authenticator,
                clientTokens,
                runtimePolicy,
                Clock.fixed(NOW, ZoneOffset.UTC),
                true
        )).build();
    }

    @Test
    void acceptsSingleBasicHeaderAndReturnsNoStoreServiceToken()
            throws Exception {
        ClientSecretAuthentication authentication =
                new ClientSecretAuthentication("orders-service-local", "secret-1");
        when(authenticator.authenticate(any(HttpServletRequest.class)))
                .thenReturn(authentication);
        when(clientTokens.issue(
                eq(authentication),
                eq(RESOURCE),
                eq("tenant-001"),
                eq(Set.of("orders:read")),
                any(Duration.class)
        )).thenReturn(new ServiceAccessToken(
                "signed-service-token",
                "Bearer",
                NOW.plusSeconds(300),
                Set.of("orders:read")
        ));

        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(HttpHeaders.AUTHORIZATION, basic(
                                "orders-service-local",
                                "secret-value"
                        ))
                        .content("grant_type=client_credentials"
                                + "&resource=" + RESOURCE
                                + "&tenant_id=tenant-001"
                                + "&scope=orders%3Aread"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "signed-service-token"
                        )
                ));

        verify(authenticator).authenticate(any(HttpServletRequest.class));
    }

    @Test
    void rejectsCredentialBearingBodyWithoutCallingBasicAuthenticator()
            throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(HttpHeaders.AUTHORIZATION, basic(
                                "orders-service-local",
                                "secret-value"
                        ))
                        .content("grant_type=client_credentials"
                                + "&client_secret=secret-value"
                                + "&resource=" + RESOURCE
                                + "&scope=orders%3Aread"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "secret-value"
                                )
                        )
                ));

        verifyNoInteractions(authenticator, clientTokens);
    }

    private static String basic(String clientId, String secret) {
        String value = clientId + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
