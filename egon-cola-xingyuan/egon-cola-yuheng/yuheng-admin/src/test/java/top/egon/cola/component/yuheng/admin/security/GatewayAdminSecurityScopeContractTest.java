package top.egon.cola.component.yuheng.admin.security;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.yuheng.admin.config.GatewayAdminServiceAuthorityFilter;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Evaluates the actual class and method annotations with Spring's method-security manager. */
class GatewayAdminSecurityScopeContractTest {
    private static final Set<String> EXPECTED = Set.of("yuheng:applications:write", "yuheng:catalog:write", "yuheng:credentials:write",
            "yuheng:drafts:write", "yuheng:groups:write", "yuheng:knowledge:admin", "yuheng:knowledge:read", "yuheng:knowledge:write",
            "yuheng:llm:read", "yuheng:llm:write", "yuheng:mcp:approve", "yuheng:mcp:read", "yuheng:mcp:runtime:read", "yuheng:mcp:test",
            "yuheng:mcp:write", "yuheng:read", "yuheng:releases:write");
    private static final List<String> CONTROLLERS = List.of(
            "application.controller.GatewayApplicationController", "catalog.controller.GatewayCatalogController",
            "credential.controller.GatewayCredentialController", "group.controller.GatewayGroupController",
            "knowledge.controller.KnowledgeController", "knowledge.controller.KnowledgeDocumentController", "knowledge.controller.KnowledgeJobController",
            "llm.controller.LlmConfigurationController", "mcp.controller.McpAppAdminController", "mcp.controller.McpApprovalController",
            "mcp.controller.McpCapabilityController", "mcp.controller.McpProtocolInspectorController", "mcp.controller.McpRemoteProviderController",
            "mcp.controller.McpServerController", "mcp.controller.McpTaskAdminController", "mcp.controller.McpToolAdminController",
            "observability.controller.GatewayObservabilityController", "openapi.controller.GatewayOpenApiController",
            "release.controller.GatewayReleaseController", "routing.controller.GatewayDraftController", "runtime.controller.GatewayProjectionController",
            "scope.controller.GatewayScopeController", "wiki.controller.WikiController");

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void sameScopeAndExisting17CapabilitiesOnEveryActualMethodOverride() throws Exception {
        var seen = new LinkedHashSet<String>();
        var pattern = Pattern.compile("'CAP_(yuheng:[A-Za-z0-9:._-]+)'");
        int checkedMethods = 0;
        for (String name : CONTROLLERS) {
            Class<?> type = Class.forName("top.egon.cola.component.yuheng.admin." + name);
            Object target = mock(type);
            for (Method method : type.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) { continue; }
                var annotation = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
                if (annotation == null) { annotation = AnnotatedElementUtils.findMergedAnnotation(type, PreAuthorize.class); }
                if (annotation == null) { continue; }
                var matcher = pattern.matcher(annotation.value());
                assertThat(matcher.find()).as(type.getSimpleName() + "." + method.getName()).isTrue();
                String permission = matcher.group(1); seen.add(permission);
                var invocation = mock(MethodInvocation.class);
                when(invocation.getMethod()).thenReturn(method); when(invocation.getThis()).thenReturn(target);
                var manager = new PreAuthorizeAuthorizationManager();
                assertThat(manager.check(() -> authentication("CAP_" + permission), invocation).isGranted())
                        .as("missing OAuth scope on " + type.getSimpleName() + "." + method.getName()).isFalse();
                assertThat(manager.check(() -> authentication("SCOPE_" + permission), invocation).isGranted()).isFalse();
                assertThat(manager.check(() -> authentication("CAP_" + permission, "SCOPE_" + permission), invocation).isGranted()).isTrue();
                assertThat(manager.check(() -> authentication("CAP_*", "SCOPE_" + permission), invocation).isGranted()).isTrue();
                checkedMethods++;
            }
        }
        assertThat(seen).isEqualTo(EXPECTED); assertThat(checkedMethods).isGreaterThan(EXPECTED.size());
    }

    @Test
    void userAndServiceProofAndScopesSurviveTheExistingServiceCapabilityAdapter() throws Exception {
        var issued = Instant.parse("2026-10-11T00:00:00Z");
        var service = new ServiceIdentityPrincipal("publisher", null, "publisher", "jti-1", URI.create("https://api.example/yuheng"), 7,
                Set.of("yuheng:read"), "biz", "publisher", "test", "credential-1", issued, issued.plusSeconds(300), "publisher-app", ServiceTokenContext.PLATFORM);
        var proof = new TokenAuditProofBO(URI.create("https://idp.example/platform"), service.resourceUri(), "a".repeat(64), "b".repeat(64), "c".repeat(64));
        var authentication = new IdpAuthenticationToken(service, List.of(new SimpleGrantedAuthority("SCOPE_yuheng:read")));
        authentication.setDetails(proof); SecurityContextHolder.getContext().setAuthentication(authentication);
        new GatewayAdminServiceAuthorityFilter().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            var adapted = SecurityContextHolder.getContext().getAuthentication();
            assertThat(adapted.getPrincipal()).isSameAs(service); assertThat(adapted.getDetails()).isSameAs(proof);
            assertThat(adapted.getAuthorities()).extracting("authority").containsExactlyInAnyOrder("CAP_yuheng:read", "SCOPE_yuheng:read");
            assertThat(adapted.getCredentials()).isEqualTo("");
        });
    }

    private org.springframework.security.core.Authentication authentication(String... values) {
        return org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("controlled-user", "",
                java.util.Arrays.stream(values).map(SimpleGrantedAuthority::new).toList());
    }
}
