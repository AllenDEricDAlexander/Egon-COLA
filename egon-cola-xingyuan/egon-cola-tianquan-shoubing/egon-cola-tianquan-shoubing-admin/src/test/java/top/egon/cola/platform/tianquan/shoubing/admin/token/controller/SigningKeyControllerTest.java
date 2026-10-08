package top.egon.cola.platform.tianquan.shoubing.admin.token.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo.SigningKeyVO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.SigningKeyService;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SigningKeyControllerTest {

    private static final TrustedRealmBO TENANT_REALM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/tenants/7"))
            .realmKey("tenant:7")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(7L)
            .businessTenantId(7L)
            .trustVersion(1L)
            .build();

    private final SigningKeyService keys = mock(SigningKeyService.class);
    private final IdpAdminAuthorizationPort authorization = mock(IdpAdminAuthorizationPort.class);
    private final OAuthManagementRealmResolver realms = mock(OAuthManagementRealmResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(realms.resolve(isNull(), isNull())).thenReturn(TENANT_REALM);
        mockMvc = MockMvcBuilders.standaloneSetup(new SigningKeyController(keys, authorization, realms))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void listsOnlyPublicKeyMetadataForResolvedRealm() throws Exception {
        when(keys.list(TENANT_REALM)).thenReturn(List.of(view()));

        mockMvc.perform(get("/api/v1/tianquan-shoubing/signing-keys"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"kid\":\"key-1\",\"issuer\":\"https://idp.example.test/tenants/7\"}]"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("privateKeyCiphertext"))));

        verify(authorization).require(isNull(), eq("tianquan-shoubing:signing-key:read"));
        verify(keys).list(TENANT_REALM);
    }

    @Test
    void publishingKeyUsesResolvedRealmAndNoStore() throws Exception {
        when(keys.publish(eq(TENANT_REALM), any())).thenReturn(view());

        mockMvc.perform(post("/api/v1/tianquan-shoubing/signing-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "kid":"key-1",
                                  "encryptedPrivateKey":"ENC:v1:test-key:AAAAAAAAAAAAAAAA:BBBBBBBBBBBBBBBBBBBBBB",
                                  "publicJwk":"{\\"kty\\":\\"RSA\\",\\"kid\\":\\"key-1\\",\\"alg\\":\\"RS256\\",\\"use\\":\\"sig\\",\\"n\\":\\"AQAB\\",\\"e\\":\\"AQAB\\"}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(authorization).require(isNull(), eq("tianquan-shoubing:signing-key:publish"));
        verify(keys).publish(eq(TENANT_REALM), any());
    }

    @Test
    void activationAndRetirementUseResolvedRealmAndDedicatedPermissions() throws Exception {
        when(keys.activate(TENANT_REALM, "key-1", 0L)).thenReturn(view());
        when(keys.retire(TENANT_REALM, "key-1", 1L)).thenReturn(view());

        mockMvc.perform(post("/api/v1/tianquan-shoubing/signing-keys/{kid}/activate", "key-1")
                        .param("expectedVersion", "0"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/tianquan-shoubing/signing-keys/{kid}/retire", "key-1")
                        .param("expectedVersion", "1"))
                .andExpect(status().isOk());

        verify(authorization).require(isNull(), eq("tianquan-shoubing:signing-key:activate"));
        verify(authorization).require(isNull(), eq("tianquan-shoubing:signing-key:retire"));
        verify(keys).activate(TENANT_REALM, "key-1", 0L);
        verify(keys).retire(TENANT_REALM, "key-1", 1L);
    }

    private static SigningKeyVO view() {
        return SigningKeyVO.builder().kid("key-1").algorithm("RS256")
                .publicJwk("{\"kty\":\"RSA\",\"kid\":\"key-1\"}")
                .status("PUBLISHED").issuer(TENANT_REALM.getIssuer().toASCIIString()).version(0L).build();
    }
}
