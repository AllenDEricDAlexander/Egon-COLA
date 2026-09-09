package top.egon.cola.platform.tianquan.shoubing.gateway.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;

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
                    "resource-tianquan-jianshen", URI.create(
                    "https://api.example/prod/permission/tianquan-jianshen"),
                    "permission", "tianquan-jianshen", "prod",
                    ResourceServerStatus.ACTIVE, 12L);

    @Test
    void resolvesExactBizAppEnvironmentAndRejectsAnotherApplication() {
        GatewayResourceServerResolver resolver = resolver();

        assertThat(resolver.resolve(Map.of(
                "tianquan-shoubing.biz-code", "permission",
                "tianquan-shoubing.app-code", "tianquan-jianshen",
                "tianquan-shoubing.env", "prod"))).isEqualTo(RESOURCE);
        assertThatThrownBy(() -> resolver.resolve(Map.of(
                "tianquan-shoubing.biz-code", "permission",
                "tianquan-shoubing.app-code", "tianquan-shoubing",
                "tianquan-shoubing.env", "prod")))
                .isInstanceOf(GatewayResourceServerResolver
                        .ResourceResolutionException.class)
                .hasMessageContaining("TIANQUAN_SHOUBING_RESOURCE_NOT_FOUND");
    }

    @Test
    void resolvesMcpResourceUriAndRejectsInactiveProjection() {
        assertThat(resolver().resolve(Map.of(
                "tianquan-shoubing.resource-uri",
                "https://api.example/prod/permission/tianquan-jianshen")))
                .isEqualTo(RESOURCE);
        GatewayResourceServerResolver inactive = new GatewayResourceServerResolver(
                key -> "resource-tianquan-jianshen",
                id -> Optional.of(new IdentityResourceServerState(
                        RESOURCE.resourceServerId(), RESOURCE.resourceUri(),
                        RESOURCE.bizCode(), RESOURCE.appCode(), RESOURCE.environment(),
                        ResourceServerStatus.DISABLED, RESOURCE.version())),
                "scope:", "uri:");

        assertThatThrownBy(() -> inactive.resolve(Map.of(
                "tianquan-shoubing.resource-uri", RESOURCE.resourceUri().toString())))
                .hasMessageContaining("TIANQUAN_SHOUBING_RESOURCE_NOT_ACTIVE");
    }

    private GatewayResourceServerResolver resolver() {
        return new GatewayResourceServerResolver(
                key -> key.startsWith("scope:")
                        && key.equals("scope:" + GatewayResourceServerResolver.sha256(
                        "permission:tianquan-jianshen:prod"))
                        || key.startsWith("uri:")
                        && key.equals("uri:" + GatewayResourceServerResolver.sha256(
                        RESOURCE.resourceUri().toString()))
                        ? "resource-tianquan-jianshen" : null,
                id -> "resource-tianquan-jianshen".equals(id)
                        ? Optional.of(RESOURCE) : Optional.empty(),
                "scope:", "uri:");
    }
}
