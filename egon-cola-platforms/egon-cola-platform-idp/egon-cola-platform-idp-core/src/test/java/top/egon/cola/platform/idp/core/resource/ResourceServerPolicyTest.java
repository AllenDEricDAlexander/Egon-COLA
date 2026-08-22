package top.egon.cola.platform.idp.core.resource;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;

import java.net.URI;
import java.time.Duration;
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
