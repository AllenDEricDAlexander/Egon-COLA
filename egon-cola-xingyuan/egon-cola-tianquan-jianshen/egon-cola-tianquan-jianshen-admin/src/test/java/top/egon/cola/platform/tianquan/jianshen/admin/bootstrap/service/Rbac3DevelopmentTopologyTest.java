package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.vo.ApplicationDefinitionVO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.Rbac3DevelopmentTopology;

class Rbac3DevelopmentTopologyTest {

    @Test
    void localAdministratorCoversManualDirectoryControllerPermissions() {
        var permissions = application("tianquan-jianshen-admin").permissions();
        for (Class<?> controller : java.util.List.of(
                top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller.UserController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.user.controller.UserDirectoryController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.application.controller.ApplicationController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.business.controller.BusinessCatalogController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.controller.UserBusinessAccessController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller.OrganizationController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.organization.controller.UserOrganizationAssignmentController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.position.controller.PositionController.class,
                top.egon.cola.platform.tianquan.jianshen.admin.iam.position.controller.UserPositionAssignmentController.class)) {
            for (var method : controller.getDeclaredMethods()) {
                var required = method.getAnnotation(top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission.class);
                if (required != null) {
                    assertThat(permissions).as("local capability for %s", method).contains(required.value());
                }
            }
        }
    }

    @Test
    void declaresEveryUnifiedIdentityApplicationAndItsAdministrativeCapabilities() {
        var applications = Rbac3DevelopmentTopology.applications();

        assertThat(applications).extracting(
                        ApplicationDefinitionVO::applicationCode)
                .containsExactly(
                        "tianquan-jianshen-admin",
                        "tianquan-shoubing-admin",
                        "yuheng-admin",
                        "tianshu-admin",
                        "mock-backend",
                        "mock-backend");
        assertThat(applications.stream()
                .flatMap(application -> application.permissions().stream())
                .allMatch(permission -> permission != null && !permission.isBlank()))
                .isTrue();
        assertThat(applications).allSatisfy(application ->
                assertThat(new HashSet<>(application.permissions()))
                        .hasSameSizeAs(application.permissions()));

        assertThat(application("tianquan-jianshen-admin").permissions()).contains(
                "system:bootstrap:read",
                "system:role-activation:read",
                "system:role-activation:use",
                "system:role-resource:read",
                "system:role-resource:manage",
                "system:tenant:target");
        assertThat(application("tianquan-jianshen-admin").permissions())
                .doesNotContain("system:tenant:read", "system:tenant:manage");
        assertThat(application("tianquan-shoubing-admin").permissions()).contains(
                "tianquan-shoubing:bootstrap:read",
                "tianquan-shoubing:identity-user:read",
                "tianquan-shoubing:oauth-client:read",
                "tianquan-shoubing:tenant:read",
                "tianquan-shoubing:tenant:manage",
                "tianquan-shoubing:resource-server:read",
                "tianquan-shoubing:resource-server:create",
                "tianquan-shoubing:resource-server:update",
                "tianquan-shoubing:resource-server:status",
                "tianquan-shoubing:resource-server:key",
                "tianquan-shoubing:resource-server:grant");
        assertThat(application("yuheng-admin").permissions()).contains(
                "yuheng:read",
                "yuheng:releases:write",
                "yuheng:mcp:read",
                "yuheng:mcp:write",
                "yuheng:mcp:test",
                "yuheng:mcp:approve",
                "yuheng:mcp:runtime:read");
        assertThat(application("tianshu-admin").permissions()).containsExactly(
                "TIANSHU_READ", "TIANSHU_WRITE", "TIANSHU_PUBLISH", "TIANSHU_CACHE");
        assertThat(application("mock-backend").permissions()).contains(
                "mock:read",
                "mock:admin",
                "mcp:unified-local:tool:local_query:call",
                "mcp:unified-local:tool:local_echo_task:call",
                "mcp:unified-local:tool:local_echo_task:task:get",
                "mcp:unified-local:tool:local_echo_task:task:update",
                "mcp:unified-local:tool:local_echo_task:task:cancel",
                "mcp:unified-local:tool:high_risk_query:call",
                "mcp:unified-local:tool:stable.remote_echo:call",
                "mcp:unified-local:tool:rc.remote_echo:call",
                "mcp:unified-local:resource:local_status:read",
                "mcp:unified-local:resource:stable.remote_text:read",
                "mcp:unified-local:resource:local_item:read",
                "mcp:unified-local:resource:qa_dashboard:read",
                "mcp:unified-local:prompt:review_item:get",
                "mcp:unified-local:prompt:rc.remote_summary:get");
        assertThat(role("mock-backend", "MOCK_LOCAL_ENTRY").permissions())
                .containsExactly("mock:read");
    }

    private static ApplicationDefinitionVO application(
            String code) {
        return Rbac3DevelopmentTopology.applications().stream()
                .filter(application -> application.applicationCode().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private static ApplicationDefinitionVO role(
            String applicationCode,
            String roleCode) {
        return Rbac3DevelopmentTopology.applications().stream()
                .filter(application -> application.applicationCode().equals(applicationCode))
                .filter(application -> application.roleCode().equals(roleCode))
                .findFirst()
                .orElseThrow();
    }
}
