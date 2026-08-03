package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3DevelopmentTopologyTest {

    @Test
    void declaresEveryUnifiedIdentityApplicationAndItsAdministrativeCapabilities() {
        var applications = Rbac3DevelopmentTopology.applications();

        assertThat(applications).extracting(
                        Rbac3DevelopmentTopology.ApplicationDefinition::applicationCode)
                .containsExactly(
                        "rbac3-admin",
                        "idp-admin",
                        "gateway-admin",
                        "ddc-admin",
                        "mock-backend");
        assertThat(applications.stream()
                .flatMap(application -> application.permissions().stream())
                .allMatch(permission -> permission != null && !permission.isBlank()))
                .isTrue();
        assertThat(applications).allSatisfy(application ->
                assertThat(new HashSet<>(application.permissions()))
                        .hasSameSizeAs(application.permissions()));

        assertThat(application("rbac3-admin").permissions()).contains(
                "system:bootstrap:read",
                "system:role-activation:read",
                "system:role-activation:use");
        assertThat(application("idp-admin").permissions()).contains(
                "idp:bootstrap:read",
                "idp:identity-user:read",
                "idp:oauth-client:read");
        assertThat(application("gateway-admin").permissions()).contains(
                "gateway:read",
                "gateway:releases:write",
                "gateway:mcp:read",
                "gateway:mcp:write",
                "gateway:mcp:test",
                "gateway:mcp:approve",
                "gateway:mcp:runtime:read");
        assertThat(application("ddc-admin").permissions()).containsExactly(
                "DDC_READ", "DDC_WRITE", "DDC_PUBLISH", "DDC_CACHE");
        assertThat(application("mock-backend").permissions()).contains(
                "mock:read",
                "mock:admin",
                "mcp:unified-local:tool:local_query:call",
                "mcp:unified-local:tool:local_query_task:call",
                "mcp:unified-local:tool:local_query_task:task:get",
                "mcp:unified-local:tool:local_query_task:task:update",
                "mcp:unified-local:tool:local_query_task:task:cancel",
                "mcp:unified-local:tool:high_risk_query:call",
                "mcp:unified-local:tool:stable.remote_echo:call",
                "mcp:unified-local:tool:rc.remote_echo:call",
                "mcp:unified-local:resource:local_status:read",
                "mcp:unified-local:resource:stable.remote_text:read",
                "mcp:unified-local:resource:local_item:read",
                "mcp:unified-local:resource:qa_dashboard:read",
                "mcp:unified-local:prompt:review_item:get",
                "mcp:unified-local:prompt:rc.remote_summary:get");
    }

    private static Rbac3DevelopmentTopology.ApplicationDefinition application(
            String code) {
        return Rbac3DevelopmentTopology.applications().stream()
                .filter(application -> application.applicationCode().equals(code))
                .findFirst()
                .orElseThrow();
    }
}
