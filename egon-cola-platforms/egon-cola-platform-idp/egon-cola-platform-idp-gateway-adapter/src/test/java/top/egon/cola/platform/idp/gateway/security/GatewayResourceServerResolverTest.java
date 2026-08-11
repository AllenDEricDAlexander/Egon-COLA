package top.egon.cola.platform.idp.gateway.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Gateway 只按受信路由三元组或 MCP Resource URI 解析当前 Resource。
 * Verifies that Gateway resolves the current Resource only from a trusted route triple or MCP
 * Resource URI.
 */
class GatewayResourceServerResolverTest {

    private static final IdentityResourceServerState RESOURCE =
            new IdentityResourceServerState(
                    "resource-rbac3", URI.create(
                    "https://api.example/prod/permission/rbac3"),
                    "permission", "rbac3", "prod",
                    ResourceServerStatus.ACTIVE, 12L);

    @Test
    void resolvesExactBizAppEnvironmentAndRejectsAnotherApplication() {
        GatewayResourceServerResolver resolver = resolver();

        assertThat(resolver.resolve(Map.of(
                "idp.biz-code", "permission",
                "idp.app-code", "rbac3",
                "idp.env", "prod"))).isEqualTo(RESOURCE);
        assertThatThrownBy(() -> resolver.resolve(Map.of(
                "idp.biz-code", "permission",
                "idp.app-code", "idp",
                "idp.env", "prod")))
                .isInstanceOf(GatewayResourceServerResolver
                        .ResourceResolutionException.class)
                .hasMessageContaining("IDP_RESOURCE_NOT_FOUND");
    }

    @Test
    void resolvesMcpResourceUriAndRejectsInactiveProjection() {
        assertThat(resolver().resolve(Map.of(
                "idp.resource-uri",
                "https://api.example/prod/permission/rbac3")))
                .isEqualTo(RESOURCE);
        GatewayResourceServerResolver inactive = new GatewayResourceServerResolver(
                key -> "resource-rbac3",
                id -> Optional.of(new IdentityResourceServerState(
                        RESOURCE.resourceServerId(), RESOURCE.resourceUri(),
                        RESOURCE.bizCode(), RESOURCE.appCode(), RESOURCE.environment(),
                        ResourceServerStatus.DISABLED, RESOURCE.version())),
                "scope:", "uri:");

        assertThatThrownBy(() -> inactive.resolve(Map.of(
                "idp.resource-uri", RESOURCE.resourceUri().toString())))
                .hasMessageContaining("IDP_RESOURCE_NOT_ACTIVE");
    }

    private GatewayResourceServerResolver resolver() {
        return new GatewayResourceServerResolver(
                key -> key.startsWith("scope:")
                        && key.equals("scope:" + GatewayResourceServerResolver.sha256(
                        "permission:rbac3:prod"))
                        || key.startsWith("uri:")
                        && key.equals("uri:" + GatewayResourceServerResolver.sha256(
                        RESOURCE.resourceUri().toString()))
                        ? "resource-rbac3" : null,
                id -> "resource-rbac3".equals(id)
                        ? Optional.of(RESOURCE) : Optional.empty(),
                "scope:", "uri:");
    }
}
