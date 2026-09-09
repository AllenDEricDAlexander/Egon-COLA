package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceAuthorizationException;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

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
 * 验收 USER 与 SERVICE Resource 授权边界以及 Tianquan-Shoubing 自身的精确 Resource 配置。
 * Accepts the USER/SERVICE Resource authorization boundary and Tianquan-Shoubing's exact Resource configuration.
 */
class OAuthResourceSecurityMatrixIT {

    private static final URI TIANQUAN_SHOUBING_URI = URI.create(
            "https://api.egon.internal/prod/permission/tianquan-shoubing");
    private static final URI TIANQUAN_JIANSHEN_URI = URI.create(
            "https://api.egon.internal/prod/permission/tianquan-jianshen");

    @Test
    void serviceGrantIsTenantAndScopeBoundWithoutConsultingUserAuthorization() {
        MatrixStore store = new MatrixStore();
        ClientCredentialsAccessPolicy policy =
                new ClientCredentialsAccessPolicy(store);

        var access = policy.authorize(
                confidentialClient(), store.rbac3, "tenant-1",
                Set.of("service:authorization:decide"));

        assertThat(access.targetResourceUri()).isEqualTo(TIANQUAN_JIANSHEN_URI);
        assertThat(access.tenantId()).isEqualTo("tenant-1");
        assertThat(access.scopes())
                .containsExactly("service:authorization:decide");
        assertThatThrownBy(() -> policy.authorize(
                confidentialClient(), store.rbac3, "tenant-2",
                Set.of("service:authorization:decide")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("TIANQUAN_SHOUBING_SERVICE_RESOURCE_GRANT_NOT_FOUND"));
        assertThatThrownBy(() -> policy.authorize(
                confidentialClient(), store.rbac3, "tenant-1",
                Set.of("service:authorization:snapshot")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("TIANQUAN_SHOUBING_SERVICE_SCOPE_INVALID"));
    }

    @Test
    void disabledTargetStopsBothUserAndServiceAuthorization() {
        MatrixStore store = new MatrixStore();
        store.rbac3 = resource(
                "permission-tianquan-jianshen-prod", TIANQUAN_JIANSHEN_URI, "tianquan-jianshen",
                "tianquan-jianshen-service", ResourceServerStatus.DISABLED);

        assertThatThrownBy(() -> new ClientCredentialsAccessPolicy(store)
                .authorize(confidentialClient(), store.rbac3, "tenant-1",
                        Set.of("service:authorization:decide")))
                .isInstanceOfSatisfying(ResourceAuthorizationException.class,
                        error -> assertThat(error.code())
                                .isEqualTo("TIANQUAN_SHOUBING_RESOURCE_SERVER_DISABLED"));
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

    private static OAuthClient confidentialClient() {
        return new OAuthClient(
                "tianquan-shoubing-service", OAuthClient.ClientType.CONFIDENTIAL,
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
                "permission-tianquan-shoubing-prod", TIANQUAN_SHOUBING_URI, "tianquan-shoubing", "tianquan-shoubing-service",
                ResourceServerStatus.ACTIVE);
        private ResourceServer rbac3 = resource(
                "permission-tianquan-jianshen-prod", TIANQUAN_JIANSHEN_URI, "tianquan-jianshen", "tianquan-jianshen-service",
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
            return "tianquan-shoubing-service".equals(clientId)
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
                    && "tianquan-shoubing-admin-web".equals(clientId)) {
                return Optional.of(new ClientResourceGrant(
                        clientId, resourceServerId, grantType, null, Set.of(),
                        ClientResourceGrant.Status.ACTIVE, 2L));
            }
            if (grantType == ResourceGrantType.CLIENT_CREDENTIALS
                    && "tianquan-shoubing-service".equals(clientId)
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
