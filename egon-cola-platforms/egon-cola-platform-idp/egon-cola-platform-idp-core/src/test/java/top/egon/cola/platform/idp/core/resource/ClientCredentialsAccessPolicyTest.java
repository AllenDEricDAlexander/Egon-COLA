package top.egon.cola.platform.idp.core.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;

import java.net.URI;
import java.time.Duration;
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
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        target = resource(
                "permission-rbac3-prod",
                "rbac3",
                "rbac3-service"
        );
        resources.resources.put(source.resourceServerId(), source);
        resources.resources.put(target.resourceServerId(), target);
        sourceClient = new OAuthClient(
                "idp-service",
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of("https://idp.example.test/internal/callback")
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
                                "rbac3:policy:read",
                                "rbac3:identity:resolve"
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
                        Set.of("rbac3:policy:read")
                );

        assertEquals(Set.of("rbac3:policy:read"), access.scopes());
        assertEquals("permission", access.sourceBizCode());
        assertEquals("idp", access.sourceAppCode());
        assertEquals("prod", access.sourceEnvironment());
    }

    @Test
    void rejectsUnknownTenantAndScopeEscalation() {
        ResourceAuthorizationException tenantFailure = assertThrows(
                ResourceAuthorizationException.class,
                () -> policy.authorize(
                        sourceClient,
                        target,
                        "tenant-002",
                        Set.of("rbac3:policy:read")
                )
        );
        assertEquals(
                "IDP_SERVICE_RESOURCE_GRANT_NOT_FOUND",
                tenantFailure.code()
        );

        ResourceAuthorizationException scopeFailure = assertThrows(
                ResourceAuthorizationException.class,
                () -> policy.authorize(
                        sourceClient,
                        target,
                        "tenant-001",
                        Set.of("rbac3:policy:write")
                )
        );
        assertEquals("IDP_SERVICE_SCOPE_INVALID", scopeFailure.code());
    }

    @Test
    void rejectsPublicOrDisabledSourceClient() {
        OAuthClient publicClient = new OAuthClient(
                "idp-service",
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of("https://idp.example.test/oauth/callback")
        );

        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        publicClient,
                        target,
                        "tenant-001",
                        Set.of("rbac3:policy:read")
                ));
        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        sourceClient.withStatus(OAuthClient.Status.DISABLED),
                        target,
                        "tenant-001",
                        Set.of("rbac3:policy:read")
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
