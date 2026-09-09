package top.egon.cola.platform.tianquan.shoubing.core.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.token.ServiceAccessTokenClaims;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientCredentialsAccessPolicyTest {

    private ResourceServerPolicyTest.FakeResourceServerStore resources;
    private ResourceServer source;
    private ResourceServer target;
    private OAuthClient sourceClient;
    private ClientCredentialsAccessPolicy policy;

    @BeforeEach
    void setUp() {
        resources = new ResourceServerPolicyTest.FakeResourceServerStore();
        source = resource(
                "permission-tianquan-shoubing-prod",
                "tianquan-shoubing",
                "tianquan-shoubing-service"
        );
        target = resource(
                "permission-tianquan-jianshen-prod",
                "tianquan-jianshen",
                "tianquan-jianshen-service"
        );
        resources.resources.put(source.resourceServerId(), source);
        resources.resources.put(target.resourceServerId(), target);
        sourceClient = new OAuthClient(
                "tianquan-shoubing-service",
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of("https://tianquan-shoubing.example.test/internal/callback")
        );
        resources.grants.put(
                ResourceServerPolicyTest.FakeResourceServerStore.key(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-001"
                ),
                new ClientResourceGrant(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-001",
                        Set.of(
                                "tianquan-jianshen:policy:read",
                                "tianquan-jianshen:identity:resolve"
                        ),
                        ClientResourceGrant.Status.ACTIVE,
                        5L
                )
        );
        policy = new ClientCredentialsAccessPolicy(resources);
    }

    @Test
    void grantsOnlyRequestedScopeSubsetForExactTenantAndTarget() {
        ClientCredentialsAccessPolicy.ServiceResourceAccess access =
                policy.authorize(
                        sourceClient,
                        target,
                        "tenant-001",
                        Set.of("tianquan-jianshen:policy:read")
                );

        assertEquals(Set.of("tianquan-jianshen:policy:read"), access.scopes());
        assertEquals("permission", access.sourceBizCode());
        assertEquals("tianquan-shoubing", access.sourceAppCode());
        assertEquals("prod", access.sourceEnvironment());
        assertEquals(ServiceTokenContext.TENANT, access.scopeContext());
        assertEquals("tenant-001", access.tenantId());
    }

    @Test
    void derivesPlatformContextFromPlatformGrantWithoutTenant() {
        resources.grants.put(
                ResourceServerPolicyTest.FakeResourceServerStore.key(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        null
                ),
                new ClientResourceGrant(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        null,
                        Set.of("tianshu:registration:write"),
                        ClientResourceGrant.Status.ACTIVE,
                        6L,
                        ServiceTokenContext.PLATFORM
                )
        );

        ClientCredentialsAccessPolicy.ServiceResourceAccess access =
                policy.authorize(
                        sourceClient,
                        target,
                        null,
                        Set.of("tianshu:registration:write")
                );

        assertEquals(ServiceTokenContext.PLATFORM, access.scopeContext());
        org.junit.jupiter.api.Assertions.assertNull(access.tenantId());
    }

    @Test
    void rejectsCallerTenantForPlatformGrantAndWildcardTenant() {
        resources.grants.remove(
                ResourceServerPolicyTest.FakeResourceServerStore.key(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-001"
                )
        );
        resources.grants.put(
                ResourceServerPolicyTest.FakeResourceServerStore.key(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        null
                ),
                new ClientResourceGrant(
                        sourceClient.clientId(),
                        target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        null,
                        Set.of("tianshu:registration:write"),
                        ClientResourceGrant.Status.ACTIVE,
                        6L,
                        ServiceTokenContext.PLATFORM
                )
        );

        assertEquals(
                "TIANQUAN_SHOUBING_SERVICE_RESOURCE_GRANT_NOT_FOUND",
                assertThrows(
                        ResourceAuthorizationException.class,
                        () -> policy.authorize(
                                sourceClient,
                                target,
                                "tenant-001",
                                Set.of("tianshu:registration:write")
                        )
                ).code()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.authorize(
                        sourceClient,
                        target,
                        "*",
                        Set.of("tianshu:registration:write")
                )
        );
    }

    @Test
    void rejectsContextAndTenantContradictionsInServiceContracts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceIdentityPrincipal(
                        "tianquan-shoubing-service",
                        "tenant-001",
                        "client-id",
                        "jti-service",
                        URI.create("https://tianshu.example.test"),
                        1L,
                        Set.of("tianshu:registration:write"),
                        "permission",
                        "tianquan-shoubing",
                        "prod",
                        "secret-1",
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(300),
                        "app-id",
                        ServiceTokenContext.PLATFORM
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceAccessTokenClaims(
                        "client-id",
                        "client-id",
                        URI.create("https://tianshu.example.test"),
                        null,
                        "permission",
                        "tianquan-shoubing",
                        "prod",
                        "secret-1",
                        1L,
                        Set.of("tianshu:registration:write"),
                        "jti-service",
                        Instant.EPOCH,
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(300),
                        "app-id",
                        ServiceTokenContext.TENANT
                )
        );
    }

    @Test
    void rejectsUnknownTenantAndScopeEscalation() {
        ResourceAuthorizationException tenantFailure = assertThrows(
                ResourceAuthorizationException.class,
                () -> policy.authorize(
                        sourceClient,
                        target,
                        "tenant-002",
                        Set.of("tianquan-jianshen:policy:read")
                )
        );
        assertEquals(
                "TIANQUAN_SHOUBING_SERVICE_RESOURCE_GRANT_NOT_FOUND",
                tenantFailure.code()
        );

        ResourceAuthorizationException scopeFailure = assertThrows(
                ResourceAuthorizationException.class,
                () -> policy.authorize(
                        sourceClient,
                        target,
                        "tenant-001",
                        Set.of("tianquan-jianshen:policy:write")
                )
        );
        assertEquals("TIANQUAN_SHOUBING_SERVICE_SCOPE_INVALID", scopeFailure.code());
    }

    @Test
    void rejectsPublicOrDisabledSourceClient() {
        OAuthClient publicClient = new OAuthClient(
                "tianquan-shoubing-service",
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of("https://tianquan-shoubing.example.test/oauth/callback")
        );

        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        publicClient,
                        target,
                        "tenant-001",
                        Set.of("tianquan-jianshen:policy:read")
                ));
        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        sourceClient.withStatus(OAuthClient.Status.DISABLED),
                        target,
                        "tenant-001",
                        Set.of("tianquan-jianshen:policy:read")
                ));
    }

    private static ResourceServer resource(
            String resourceServerId,
            String appCode,
            String managementClientId) {
        return new ResourceServer(
                resourceServerId,
                URI.create(
                        "https://api.egon.internal/prod/permission/"
                                + appCode
                ),
                "permission",
                appCode,
                "prod",
                managementClientId,
                appCode,
                appCode + ":access",
                Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE,
                2L
        );
    }
}
