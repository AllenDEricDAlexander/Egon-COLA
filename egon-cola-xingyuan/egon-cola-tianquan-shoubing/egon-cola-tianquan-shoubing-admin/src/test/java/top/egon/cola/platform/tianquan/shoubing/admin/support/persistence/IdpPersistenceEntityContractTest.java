package top.egon.cola.platform.tianquan.shoubing.admin.support.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

import static org.junit.jupiter.api.Assertions.*;

class IdpPersistenceEntityContractTest {

    private static final Instant NOW = Instant.parse("2026-10-09T01:00:00Z");

    @Test
    void mpClientMappingPreservesHistoryAndExactRedirectWhileSasRequiresPkce() {
        OAuthClientPO source = new OAuthClientPO();
        source.setId(1001L);
        source.setVersion(7L);
        source.setCreateTime(NOW);
        source.setUpdateTime(NOW.plusSeconds(1));
        source.setClientId("browser-client").setClientName("Browser Client")
                .setClientType("PUBLIC").setPurpose(OAuthClientPurposeEnum.USER)
                .setStatus("ACTIVE").setCredentialVersion(0L)
                .setRedirectUris(List.of("https://ui.example/callback"))
                .setResourceUris(List.of("https://api.example/orders"))
                .setAllowedScopes(List.of("openid", "orders:read"))
                .setAccessTtlSeconds(300).setRefreshTtlSeconds(0);
        OAuthClientConverter converter = Mappers.getMapper(OAuthClientConverter.class);
        OAuthClientBO client = converter.toTarget(source);
        assertEquals(1001L, client.getId());
        assertEquals(7L, client.getVersion());
        assertEquals(NOW, client.getCreatedAt());
        assertEquals(NOW.plusSeconds(1), client.getUpdatedAt());
        assertEquals(List.of("https://ui.example/callback"), client.getRedirectUris());
        var registered = Mappers.getMapper(SasRegisteredClientConverter.class).toTarget(client);
        assertTrue(registered.getClientSettings().isRequireProofKey());
        assertTrue(registered.getClientSettings().isRequireAuthorizationConsent());
        assertEquals(java.util.Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                registered.getAuthorizationGrantTypes());
        assertNull(converter.toSource(client).getCreateTime());
        client.setAccessTtlSeconds(900);
        assertThrows(IllegalArgumentException.class,
                () -> Mappers.getMapper(SasRegisteredClientConverter.class).toTarget(client));
    }

    @Test
    void mpResourceAndGrantMappingKeepApplicationAndExactScopeVersionBoundaries() {
        OAuthResourcePO resource = new OAuthResourcePO();
        resource.setId(2001L);
        resource.setVersion(3L);
        resource.setCreateTime(NOW);
        resource.setUpdateTime(NOW.plusSeconds(1));
        resource.setResourceServerId("orders-resource").setResourceUri("https://api.example/orders")
                .setAppCode("orders").setBizCode("business").setEnvironment("prod");
        var resourceBo = Mappers.getMapper(OAuthResourceConverter.class).toTarget(resource);
        assertEquals("orders-resource", resourceBo.getResourceServerId());
        assertEquals(java.net.URI.create("https://api.example/orders"), resourceBo.getResourceUri());
        assertEquals("orders", resourceBo.getAppCode());
        assertEquals(NOW, resourceBo.getCreatedAt());
        OAuthGrantPO grant = new OAuthGrantPO();
        grant.setId(3001L);
        grant.setVersion(4L);
        grant.setClientPk(1001L).setResourceServerId("orders-resource")
                .setGrantType("CLIENT_CREDENTIALS")
                .setAllowedScopes(List.of("orders:read"))
                .setStatus("ACTIVE").setResourceVersion(3L);
        var grantBo = Mappers.getMapper(OAuthGrantConverter.class).toTarget(grant);
        assertEquals(1001L, grantBo.getClientPk());
        assertEquals(4L, grantBo.getVersion());
        assertEquals(3L, grantBo.getResourceVersion());
        assertEquals(List.of("orders:read"), grantBo.getAllowedScopes());
        assertEquals(ResourceGrantType.CLIENT_CREDENTIALS, grantBo.getGrantType());
        assertEquals(grantBo.getAllowedScopes(),
                Mappers.getMapper(OAuthGrantConverter.class).toSource(grantBo).getAllowedScopes());
    }

    @Test
    void mpSigningMetadataKeepsStateAndTimeWithoutExposingPrivateMaterial() throws Exception {
        OAuthSigningKeyPO source = new OAuthSigningKeyPO();
        source.setId(4001L);
        source.setVersion(5L);
        source.setCreateTime(NOW);
        source.setUpdateTime(NOW.plusSeconds(1));
        source.setKid("key-1").setAlgorithm("RS256").setStatus(OAuthKeyStatusEnum.PUBLISHED)
                .setPublicJwk(new ObjectMapper().readTree("{\"kty\":\"RSA\",\"kid\":\"key-1\"}"))
                .setPrivateKeyCiphertext("ENC:v1:kek:protected:private-material").setPublishedAt(NOW);
        OAuthSigningKeyBO key = Mappers.getMapper(OAuthSigningKeyConverter.class).toTarget(source);
        assertEquals(OAuthKeyStatusEnum.PUBLISHED, key.getStatus());
        assertEquals(NOW, key.getPublishedAt());
        assertEquals(NOW, key.getCreatedAt());
        assertEquals(5L, key.getVersion());
        assertEquals("RS256", key.getAlgorithm());
        assertFalse(new ObjectMapper().findAndRegisterModules().writeValueAsString(key)
                .contains("private-material"));
        assertFalse(key.toString().contains("private-material"));
    }
}
