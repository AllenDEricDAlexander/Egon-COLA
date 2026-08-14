package top.egon.cola.platform.idp.core.resource;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceServerPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");
    private static final URI RESOURCE_URI = URI.create(
            "https://api.egon.internal/prod/permission/idp");

    @Test
    void validatesResourceIdentifierAndExactApplicationScope() {
        ResourceServer resource = resource(ResourceServerStatus.ACTIVE);

        assertTrue(resource.matches("permission", "idp", "prod"));
        assertFalse(resource.matches("permission", "rbac3", "prod"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceServer(
                "permission-idp-prod",
                URI.create("/permission/idp"),
                "permission",
                "idp",
                "prod",
                "idp-service",
                "idp",
                "idp:access",
                Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE,
                1L
        ));
        assertThrows(IllegalArgumentException.class, () -> new ResourceServer(
                "permission-idp-prod",
                URI.create("https://api.egon.internal/prod/permission/idp#fragment"),
                "permission",
                "idp",
                "prod",
                "idp-service",
                "idp",
                "idp:access",
                Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE,
                1L
        ));
    }

    @Test
    void enforcesMutuallyExclusiveGrantFacts() {
        ClientResourceGrant userGrant = new ClientResourceGrant(
                "idp-admin-web",
                "permission-idp-prod",
                ResourceGrantType.USER_DELEGATION,
                null,
                Set.of(),
                ClientResourceGrant.Status.ACTIVE,
                1L
        );

        assertTrue(userGrant.active());
        assertThrows(IllegalArgumentException.class, () ->
                new ClientResourceGrant(
                        "idp-admin-web",
                        "permission-idp-prod",
                        ResourceGrantType.USER_DELEGATION,
                        "tenant-001",
                        Set.of(),
                        ClientResourceGrant.Status.ACTIVE,
                        1L
                ));
        assertThrows(IllegalArgumentException.class, () ->
                new ClientResourceGrant(
                        "idp-service",
                        "permission-rbac3-prod",
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-001",
                        Set.of(),
                        ClientResourceGrant.Status.ACTIVE,
                        1L
                ));
    }

    @Test
    void admitsOnlyTheRegisteredTripleWithAnActiveConfidentialCredential() {
        ResourceServer resource = resource(ResourceServerStatus.ACTIVE);
        OAuthClient client = confidentialClient("idp-service");
        ClientJwkCredential credential = credential(
                "idp-service",
                ClientJwkCredential.Status.ACTIVE
        );
        ResourceServerAdmissionPolicy policy =
                new ResourceServerAdmissionPolicy();

        ResourceServerAdmissionPolicy.AdmissionAuthorization result =
                policy.authorize(
                        resource,
                        client,
                        credential,
                        "permission",
                        "idp",
                        "prod",
                        "idp-admin-01",
                        NOW
                );

        assertEquals("permission-idp-prod", result.resourceServerId());
        assertEquals("key-2026-01", result.credentialId());
        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        resource,
                        client,
                        credential,
                        "permission",
                        "rbac3",
                        "prod",
                        "idp-admin-01",
                        NOW
                ));
        assertThrows(ResourceAuthorizationException.class, () ->
                policy.authorize(
                        resource,
                        client,
                        credential(
                                "idp-service",
                                ClientJwkCredential.Status.DISABLED
                        ),
                        "permission",
                        "idp",
                        "prod",
                        "idp-admin-01",
                        NOW
                ));
    }

    @Test
    void admissionPolicyRejectsAResourceUriDifferentFromTheRegisteredOne() {
        ResourceServer resource = resource(ResourceServerStatus.ACTIVE);
        ResourceServerAdmissionPolicy policy =
                new ResourceServerAdmissionPolicy();
        AdmissionRequest wrongResource = new AdmissionRequest(
                resource.resourceServerId(),
                java.net.URI.create("https://api.example/rbac3"),
                resource.bizCode(),
                resource.appCode(),
                resource.environment(),
                "idp-admin-01"
        );

        ResourceAuthorizationException exception = assertThrows(
                ResourceAuthorizationException.class,
                () -> policy.authorize(
                        resource,
                        confidentialClient("idp-service"),
                        credential(
                                "idp-service",
                                ClientJwkCredential.Status.ACTIVE
                        ),
                        wrongResource,
                        NOW
                )
        );

        assertEquals("IDP_RESOURCE_SERVER_URI_MISMATCH", exception.code());
    }

    @Test
    void userPolicyRequiresResourceGrantMembershipAndRbacEntryPermission() {
        FakeResourceServerStore resources = new FakeResourceServerStore();
        ResourceServer resource = resource(ResourceServerStatus.ACTIVE);
        resources.resources.put(resource.resourceServerId(), resource);
        resources.grants.put(
                FakeResourceServerStore.key(
                        "idp-admin-web",
                        resource.resourceServerId(),
                        ResourceGrantType.USER_DELEGATION,
                        null
                ),
                new ClientResourceGrant(
                        "idp-admin-web",
                        resource.resourceServerId(),
                        ResourceGrantType.USER_DELEGATION,
                        null,
                        Set.of(),
                        ClientResourceGrant.Status.ACTIVE,
                        1L
                )
        );
        TenantMembershipPort memberships = new TenantMembershipPort() {
            @Override
            public TenantMembership resolve(
                    String identitySub,
                    String tenantId) {
                return new TenantMembership(
                        identitySub,
                        tenantId,
                        "rbac-user-1",
                        "Tenant 001",
                        MembershipStatus.ACTIVE
                );
            }

            @Override
            public List<TenantMembership> list(String identitySub) {
                return List.of();
            }
        };
        UserResourceAccessAuthorizationPort authorization = request ->
                new UserResourceAccessAuthorizationPort.AccessDecision(
                        UserResourceAccessAuthorizationPort.Decision.ALLOW,
                        "ENTRY_PERMISSION_GRANTED",
                        7L,
                        11L,
                        13L
                );
        UserResourceAccessPolicy policy = new UserResourceAccessPolicy(
                resources,
                memberships,
                authorization
        );

        UserResourceAccessPolicy.UserResourceAccess access = policy.authorize(
                publicClient(),
                RESOURCE_URI,
                "alice-sub",
                "tenant-001"
        );

        assertEquals(resource.resourceServerId(), access.resourceServerId());
        assertEquals(7L, access.authorizationVersion());
    }

    private static ResourceServer resource(ResourceServerStatus status) {
        return new ResourceServer(
                "permission-idp-prod",
                RESOURCE_URI,
                "permission",
                "idp",
                "prod",
                "idp-service",
                "idp",
                "idp:access",
                Duration.ofMinutes(5),
                status,
                3L
        );
    }

    private static OAuthClient publicClient() {
        return new OAuthClient(
                "idp-admin-web",
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of("https://idp.example.test/oauth/callback")
        );
    }

    private static OAuthClient confidentialClient(String clientId) {
        return new OAuthClient(
                clientId,
                OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE,
                false,
                List.of("https://idp.example.test/internal/callback")
        );
    }

    private static ClientJwkCredential credential(
            String clientId,
            ClientJwkCredential.Status status) {
        return new ClientJwkCredential(
                clientId,
                "key-2026-01",
                "RS256",
                "{\"kty\":\"RSA\",\"kid\":\"key-2026-01\"}",
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600),
                status,
                null,
                1L
        );
    }

    static final class FakeResourceServerStore
            implements ResourceServerStore {

        final Map<String, ResourceServer> resources = new HashMap<>();
        final Map<String, ClientResourceGrant> grants =
                new HashMap<>();

        static String key(
                String clientId,
                String resourceServerId,
                ResourceGrantType grantType,
                String tenantId) {
            return clientId + ':' + resourceServerId + ':' + grantType
                    + ':' + String.valueOf(tenantId);
        }

        @Override
        public Optional<ResourceServer> findById(String resourceServerId) {
            return Optional.ofNullable(resources.get(resourceServerId));
        }

        @Override
        public Optional<ResourceServer> findByUri(URI resourceUri) {
            return resources.values().stream()
                    .filter(value -> value.resourceUri().equals(resourceUri))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByScope(
                String bizCode,
                String appCode,
                String environment) {
            return resources.values().stream()
                    .filter(value -> value.matches(
                            bizCode,
                            appCode,
                            environment
                    ))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByManagementClientId(
                String clientId) {
            return resources.values().stream()
                    .filter(value -> value.managementClientId().equals(
                            clientId
                    ))
                    .findFirst();
        }

        @Override
        public Optional<ClientResourceGrant> findGrant(
                String clientId,
                String resourceServerId,
                ResourceGrantType grantType,
                String tenantId) {
            return Optional.ofNullable(grants.get(key(
                    clientId,
                    resourceServerId,
                    grantType,
                    tenantId
            )));
        }
    }
}
