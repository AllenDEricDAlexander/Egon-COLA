package top.egon.cola.platform.idp.admin.oauth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthClientControllerTest {

    private final OAuthClientService clients = mock(OAuthClientService.class);
    private final IdpAdminAuthorizationPort authorization =
            mock(IdpAdminAuthorizationPort.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new OAuthClientController(clients, authorization)
        ).setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
        ).build();
    }

    @Test
    void createsClientWithCreatePermissionAndNoStoreSecretResponse()
            throws Exception {
        when(clients.create(any(), anyString())).thenReturn(created());

        mockMvc.perform(post("/api/v1/identity/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appId":"orders-service",
                                  "clientId":"orders-service-local",
                                  "clientName":"Orders Service",
                                  "clientType":"CONFIDENTIAL",
                                  "accessTokenTtlSeconds":900,
                                  "refreshTokenTtlSeconds":86400,
                                  "redirectUris":[],
                                  "resourceUris":[]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(authorization).require(
                isNull(),
                eq("idp:oauth-client:create")
        );
    }

    @Test
    void rotatesClientSecretWithUpdatePermissionAndNoStoreResponse()
            throws Exception {
        when(clients.rotateSecret(
                eq("orders-service-local"),
                any(),
                anyString()
        ))
                .thenReturn(rotated());

        mockMvc.perform(post(
                        "/api/v1/identity/clients/{clientId}/secret-rotations",
                        "orders-service-local"
                ).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(authorization).require(
                isNull(),
                eq("idp:oauth-client:update")
        );
    }

    @Test
    void listsClientsWithReadPermissionAndSafeViewContract()
            throws Exception {
        when(clients.list()).thenReturn(List.of(new OAuthClientVO(
                "orders-service-local",
                "Orders Service",
                "CONFIDENTIAL",
                "ACTIVE",
                false,
                900,
                86400,
                List.of(),
                List.of(),
                0L,
                Instant.EPOCH,
                Instant.EPOCH,
                "orders-service",
                "7Kp2",
                "ACTIVE"
        )));

        mockMvc.perform(get("/api/v1/identity/clients"))
                .andExpect(status().isOk());

        verify(authorization).require(
                isNull(),
                eq("idp:oauth-client:read")
        );
    }

    private static CreatedOAuthClientVO created() {
        return new CreatedOAuthClientVO(
                "orders-service-local",
                "orders-service",
                "Orders Service",
                "CONFIDENTIAL",
                "ACTIVE",
                "one-time-secret",
                "7Kp2",
                0L,
                Instant.EPOCH
        );
    }

    private static RotatedClientSecretVO rotated() {
        return new RotatedClientSecretVO(
                "orders-service-local",
                "orders-service",
                "new-one-time-secret",
                "yQ8m",
                1L,
                Instant.EPOCH
        );
    }
}
