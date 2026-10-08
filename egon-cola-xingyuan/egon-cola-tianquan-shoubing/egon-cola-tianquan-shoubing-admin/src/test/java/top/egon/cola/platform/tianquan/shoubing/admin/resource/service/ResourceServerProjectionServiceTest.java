package top.egon.cola.platform.tianquan.shoubing.admin.resource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBatch;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ResourceServerProjectionServiceTest {

    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RBatch batch = mock(RBatch.class);
    private final RBucketAsync<String> bucket = mock(RBucketAsync.class);
    private ResourceServerProjectionService projections;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        when(redisson.createBatch(any())).thenReturn(batch);
        when(batch.<String>getBucket(anyString(), eq(StringCodec.INSTANCE))).thenReturn(bucket);
        projections = new ResourceServerProjectionService(redisson, objectMapper);
    }

    @Test
    void projectsResourceUriScopeAndBoundClientInOneRedisBatch() {
        OAuthResourceBO resource = resource();
        projections.projectResource(resource, managementClient());

        verify(batch).getBucket("identity:resource-server:permission-tianquan-shoubing-prod", StringCodec.INSTANCE);
        verify(batch).getBucket("identity:resource-uri:" + sha256(resource.getResourceUri().toASCIIString()),
                StringCodec.INSTANCE);
        verify(batch).getBucket("identity:resource-scope:" + sha256("permission:tianquan-shoubing:prod"),
                StringCodec.INSTANCE);
        verify(batch).getBucket("identity:oauth-client:tianquan-shoubing-service", StringCodec.INSTANCE);
        verify(batch).execute();
    }

    @Test
    void projectsServiceGrantByClientResourceAndTenant() {
        projections.projectServiceGrant(tenant(), managementClient(), resource(), grant());

        verify(batch).getBucket("identity:service-resource-grant:tianquan-shoubing-service:"
                + "permission-tianquan-shoubing-prod:7", StringCodec.INSTANCE);
        verify(batch).execute();
    }

    @Test
    void rejectsUserDelegationProjection() {
        OAuthGrantBO userGrant = grant().setGrantType(ResourceGrantType.USER_DELEGATION);
        assertThatThrownBy(() -> projections.projectServiceGrant(tenant(), managementClient(), resource(), userGrant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ONLY_SERVICE_GRANTS_ARE_PROJECTED");
    }

    @Test
    void projectionFailureFailsTheManagementMutation() {
        when(batch.execute()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> projections.projectResource(resource(), managementClient()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("redis unavailable");
    }

    private static OAuthResourceBO resource() {
        return OAuthResourceBO.builder()
                .id(1L)
                .version(0L)
                .resourceServerId("permission-tianquan-shoubing-prod")
                .resourceUri(URI.create("https://api.egon.internal/prod/permission/tianquan-shoubing"))
                .bizCode("permission")
                .appCode("tianquan-shoubing")
                .environment("prod")
                .displayName("Tianquan-Shoubing Production")
                .managementClientId("tianquan-shoubing-service")
                .managementClientPk(11L)
                .rbacApplicationCode("tianquan-shoubing")
                .entryPermissionCode("tianquan-shoubing:access")
                .status(ResourceServerStatus.ACTIVE)
                .admissionTicketTtl(Duration.ofMinutes(5))
                .build();
    }

    private static OAuthClientBO managementClient() {
        return OAuthClientBO.builder()
                .id(11L)
                .version(0L)
                .clientId("tianquan-shoubing-service")
                .clientName("Tianquan-Shoubing Service")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE)
                .status(OAuthClient.Status.ACTIVE)
                .credentialVersion(0L)
                .redirectUris(List.of())
                .resourceUris(List.of())
                .allowedScopes(List.of("service:authorization:decide"))
                .accessTtlSeconds(600)
                .refreshTtlSeconds(0)
                .build();
    }

    private static OAuthGrantBO grant() {
        return OAuthGrantBO.builder()
                .id(1L)
                .version(0L)
                .clientPk(11L)
                .resourceServerId("permission-tianquan-shoubing-prod")
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of("service:authorization:decide"))
                .status(ClientResourceGrant.Status.ACTIVE)
                .resourceVersion(0L)
                .build();
    }

    private static TrustedRealmBO tenant() {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example.test/tenants/7"))
                .realmKey("tenant:7")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(7L)
                .businessTenantId(7L)
                .trustVersion(0L)
                .build();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
