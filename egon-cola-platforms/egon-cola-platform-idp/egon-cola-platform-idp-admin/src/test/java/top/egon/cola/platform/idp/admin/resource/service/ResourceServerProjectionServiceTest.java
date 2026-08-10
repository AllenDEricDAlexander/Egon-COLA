package top.egon.cola.platform.idp.admin.resource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBatch;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ResourceServerProjectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RBatch batch = mock(RBatch.class);
    private final RBucketAsync<String> bucket = mock(RBucketAsync.class);

    private ResourceServerProjectionService projections;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        when(redisson.createBatch(any())).thenReturn(batch);
        when(batch.<String>getBucket(anyString(), eq(StringCodec.INSTANCE)))
                .thenReturn(bucket);
        projections = new ResourceServerProjectionService(
                redisson,
                objectMapper
        );
    }

    @Test
    void projectsResourceUriScopeAndBoundClientInOneRedisBatch() {
        IdentityResourceServerEntity resource = resource();
        IdentityClientEntity client = IdentityClientEntity.createPublic(
                "idp-service",
                "IdP Service",
                900,
                604_800,
                NOW
        );

        projections.projectResource(resource, client);

        verify(batch).getBucket(
                "identity:resource-server:permission-idp-prod",
                StringCodec.INSTANCE
        );
        verify(batch).getBucket(
                "identity:resource-uri:" + sha256(resource.getResourceUri()),
                StringCodec.INSTANCE
        );
        verify(batch).getBucket(
                "identity:resource-scope:"
                        + sha256("permission:idp:prod"),
                StringCodec.INSTANCE
        );
        verify(batch).getBucket(
                "identity:oauth-client:idp-service",
                StringCodec.INSTANCE
        );
        verify(batch).execute();
    }

    @Test
    void projectsServiceGrantByClientResourceAndTenant() {
        IdentityClientResourceGrantEntity grant =
                IdentityClientResourceGrantEntity.clientCredentials(
                        "grant-row",
                        "idp-service",
                        "permission-rbac3-prod",
                        "tenant-1",
                        "[\"rbac3:policy:read\"]",
                        NOW
                );

        projections.projectServiceGrant(grant);

        verify(batch).getBucket(
                "identity:service-resource-grant:idp-service:"
                        + "permission-rbac3-prod:tenant-1",
                StringCodec.INSTANCE
        );
        verify(batch).execute();
    }

    @Test
    void projectionFailureFailsTheManagementMutation() {
        when(batch.execute()).thenThrow(new IllegalStateException(
                "redis unavailable"
        ));

        assertThatThrownBy(() -> projections.projectResource(
                resource(),
                IdentityClientEntity.createPublic(
                        "idp-service",
                        "IdP Service",
                        900,
                        604_800,
                        NOW
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("redis unavailable");
    }

    private static IdentityResourceServerEntity resource() {
        return IdentityResourceServerEntity.create(
                "resource-row",
                "permission-idp-prod",
                "https://api.egon.internal/prod/permission/idp",
                "permission",
                "idp",
                "prod",
                "IdP Production",
                "idp-service",
                "idp",
                "idp:access",
                300,
                IdentityResourceServerEntity.Status.ACTIVE,
                NOW
        );
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
