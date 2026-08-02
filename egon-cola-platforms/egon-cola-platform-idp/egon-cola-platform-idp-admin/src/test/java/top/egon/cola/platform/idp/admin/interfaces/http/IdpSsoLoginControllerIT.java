package top.egon.cola.platform.idp.admin.interfaces.http;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.security.IdpAdminSecurityConfiguration;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdpSsoLoginController.class)
@Import({IdpAdminSecurityConfiguration.class, IdpHttpExceptionHandler.class})
class IdpSsoLoginControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdentityFacade identities;

    @MockitoBean
    private IdpSsoSessionStore sessions;

    @MockitoBean
    private SecureRandom random;

    @MockitoBean(name = "idpClock")
    private Clock clock;

    @BeforeEach
    void configureClockAndRandom() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-02T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        doAnswer(invocation -> {
            Arrays.fill(invocation.<byte[]>getArgument(0), (byte) 9);
            return null;
        }).when(random).nextBytes(org.mockito.ArgumentMatchers.any(byte[].class));
    }

    @Test
    void csrfEndpointIsPublicAndSetsDoubleSubmitCookie() throws Exception {
        mockMvc.perform(get("/oauth2/login/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Set-Cookie",
                        containsString("EGON_IDP_CSRF=")
                ))
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.not(containsString("HttpOnly"))
                ))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void passwordLoginUsesCustomCsrfAndSetsHostOnlySsoCookie() throws Exception {
        when(identities.authenticate(
                eq("alice"),
                any(char[].class),
                eq("browser-127.0.0.1"),
                eq(Instant.parse("2026-08-02T00:00:00Z"))
        )).thenReturn(new AuthenticatedIdentity(
                "alice-sub",
                "alice",
                "Alice",
                1L,
                false
        ));
        when(sessions.create(eq("alice-sub"), any()))
                .thenReturn("sso-token");

        mockMvc.perform(post("/oauth2/login")
                        .header("X-IDP-CSRF", "csrf-token")
                        .cookie(new Cookie("EGON_IDP_CSRF", "csrf-token"))
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Set-Cookie",
                        containsString("HttpOnly")
                ))
                .andExpect(jsonPath("$.identitySub").value("alice-sub"))
                .andExpect(result -> {
                    var cookies = result.getResponse().getHeaders("Set-Cookie");
                    assertTrue(cookies.stream().anyMatch(value -> value.contains(
                            "EGON_IDP_SSO=sso-token;"
                    )));
                    assertTrue(cookies.stream().anyMatch(value -> value.contains(
                            "EGON_IDP_CSRF=;"
                    )));
                });
    }

    @Test
    void rejectsMismatchedDoubleSubmitCsrf() throws Exception {
        mockMvc.perform(post("/oauth2/login")
                        .header("X-IDP-CSRF", "header-token")
                        .cookie(new Cookie("EGON_IDP_CSRF", "cookie-token"))
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","password":"secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
