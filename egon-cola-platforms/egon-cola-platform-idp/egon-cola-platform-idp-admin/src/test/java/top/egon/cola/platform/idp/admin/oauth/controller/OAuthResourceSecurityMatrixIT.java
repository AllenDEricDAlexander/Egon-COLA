package top.egon.cola.platform.idp.admin.oauth.controller;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验收 USER 与 SERVICE Resource 授权边界以及 IdP 自身的精确 Resource 配置。
 * Accepts the USER/SERVICE Resource authorization boundary and IdP's exact Resource configuration.
 */
class OAuthResourceSecurityMatrixIT {

    private static final URI IDP_URI = URI.create(
            "https://api.egon.internal/prod/permission/idp");
    private static final URI RBAC3_URI = URI.create(
            "https://api.egon.internal/prod/permission/rbac3");

    @Test
    void userEntryPermissionIsEvaluatedForTheRequestedApplicationOnly() {
        MatrixStore store = new MatrixStore();
        UserResourceAccessPolicy policy = new UserResourceAccessPolicy(
                store,
                memberships(),
                request -> new UserResourceAccessAuthorizationPort.AccessDecision(
                        "idp".equals(request.rbacApplicationCode())
                                ? UserResourceAccessAuthorizationPort.Decision.ALLOW
                                : UserResourceAccessAuthorizationPort.Decision.DENY,
                        "idp".equals(request.rbacApplicationCode())
                                ? "ALLOW" : "ENTRY_PERMISSION_DENIED",
                        11L, 12L, 13L));
        OAuthClient client = publicClient();

        assertThat(policy.authorize(
                        client, IDP_URI, "alice", "tenant-1")
                .resourceUri()).isEqualTo(IDP_URI);
        assertThatThrownBy(() -> policy.authorize(
                client, RBAC3_URI, "alice", "tenant-1"))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("IDP_RESOURCE_ACCESS_DENIED"));
    }

    @Test
    void serviceGrantIsTenantAndScopeBoundWithoutConsultingUserAuthorization() {
        MatrixStore store = new MatrixStore();
        ClientCredentialsAccessPolicy policy =
                new ClientCredentialsAccessPolicy(store);

        var access = policy.authorize(
                confidentialClient(), store.rbac3, "tenant-1",
                Set.of("service:authorization:decide"));

        assertThat(access.targetResourceUri()).isEqualTo(RBAC3_URI);
        assertThat(access.tenantId()).isEqualTo("tenant-1");
        assertThat(access.scopes())
                .containsExactly("service:authorization:decide");
        assertThatThrownBy(() -> policy.authorize(
                confidentialClient(), store.rbac3, "tenant-2",
                Set.of("service:authorization:decide")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("IDP_SERVICE_RESOURCE_GRANT_NOT_FOUND"));
        assertThatThrownBy(() -> policy.authorize(
                confidentialClient(), store.rbac3, "tenant-1",
                Set.of("service:authorization:snapshot")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("IDP_SERVICE_SCOPE_INVALID"));
    }

    @Test
    void disabledTargetStopsBothUserAndServiceAuthorization() {
        MatrixStore store = new MatrixStore();
        store.rbac3 = resource(
                "permission-rbac3-prod", RBAC3_URI, "rbac3",
                "rbac3-service", ResourceServerStatus.DISABLED);

        assertThatThrownBy(() -> new ClientCredentialsAccessPolicy(store)
                .authorize(confidentialClient(), store.rbac3, "tenant-1",
                        Set.of("service:authorization:decide")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("IDP_RESOURCE_SERVER_DISABLED"));
    }

    @Test
    void applicationUsesOneConfiguredResourceInsteadOfStaticAudienceLists()
            throws IOException {
        String yaml;
        try (var input = getClass().getResourceAsStream("/application.yml")) {
            assertThat(input).isNotNull();
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(yaml).contains("resource-server-id:", "resource-uri:")
                .doesNotContain("audiences:", "client-ids:");
    }

    private static OAuthClient publicClient() {
        return new OAuthClient(
                "idp-admin-web", OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE, true,
                List.of("https://idp.example/oauth/callback"));
    }

    private static TenantMembershipPort memberships() {
        return new TenantMembershipPort() {
            @Override
            public TenantMembership resolve(
                    String identitySub,
                    String tenantId
            ) {
                return new TenantMembership(
                        identitySub, tenantId, "rbac-user-1", "Tenant 1",
                        MembershipStatus.ACTIVE);
            }

            @Override
            public List<TenantMembership> list(String identitySub) {
                return List.of(resolve(identitySub, "tenant-1"));
            }
        };
    }

    private static OAuthClient confidentialClient() {
        return new OAuthClient(
                "idp-service", OAuthClient.ClientType.CONFIDENTIAL,
                OAuthClient.Status.ACTIVE, false, List.of());
    }

    private static ResourceServer resource(
            String id,
            URI uri,
            String app,
            String managementClient,
            ResourceServerStatus status
    ) {
        return new ResourceServer(
                id, uri, "permission", app, "prod", managementClient,
                app, app + ":entry", Duration.ofMinutes(5), status, 3L);
    }

    private static final class MatrixStore implements ResourceServerStore {

        private final ResourceServer idp = resource(
                "permission-idp-prod", IDP_URI, "idp", "idp-service",
                ResourceServerStatus.ACTIVE);
        private ResourceServer rbac3 = resource(
                "permission-rbac3-prod", RBAC3_URI, "rbac3", "rbac3-service",
                ResourceServerStatus.ACTIVE);

        @Override
        public Optional<ResourceServer> findById(String resourceServerId) {
            return List.of(idp, rbac3).stream()
                    .filter(resource -> resource.resourceServerId()
                            .equals(resourceServerId))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByUri(URI resourceUri) {
            return List.of(idp, rbac3).stream()
                    .filter(resource -> resource.resourceUri().equals(resourceUri))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByScope(
                String bizCode,
                String appCode,
                String environment
        ) {
            return List.of(idp, rbac3).stream()
                    .filter(resource -> resource.matches(
                            bizCode, appCode, environment))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByManagementClientId(
                String clientId
        ) {
            return "idp-service".equals(clientId)
                    ? Optional.of(idp) : Optional.empty();
        }

        @Override
        public Optional<ClientResourceGrant> findGrant(
                String clientId,
                String resourceServerId,
                ResourceGrantType grantType,
                String tenantId
        ) {
            if (grantType == ResourceGrantType.USER_DELEGATION
                    && "idp-admin-web".equals(clientId)) {
                return Optional.of(new ClientResourceGrant(
                        clientId, resourceServerId, grantType, null, Set.of(),
                        ClientResourceGrant.Status.ACTIVE, 2L));
            }
            if (grantType == ResourceGrantType.CLIENT_CREDENTIALS
                    && "idp-service".equals(clientId)
                    && rbac3.resourceServerId().equals(resourceServerId)
                    && "tenant-1".equals(tenantId)) {
                return Optional.of(new ClientResourceGrant(
                        clientId, resourceServerId, grantType, tenantId,
                        Set.of("service:authorization:decide"),
                        ClientResourceGrant.Status.ACTIVE, 4L));
            }
            return Optional.empty();
        }
    }
}
