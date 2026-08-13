package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import top.egon.cola.platform.rbac3.admin.tenant.controller.TenantController;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.tenant.service.TenantContextResolver;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rbac3TenantIsolationIT {

    @Test
    void targetTenantUsesOneCanonicalHeaderAndPlatformRouteBoundary()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/rbac3/v1/platform/tenants/20002");
        request.addHeader(TenantContextResolver.TARGET_HEADER, "20002");
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal(), "n/a", java.util.List.of());

        var context = new TenantContextResolver().resolve(request, authentication);

        assertEquals("20001", context.authenticatedTenantId());
        assertEquals("20002", context.effectiveTenantId());
        assertTrue(context.platformTarget());

        Path webSource = Path.of(System.getProperty("basedir")).getParent()
                .resolve("egon-cola-platform-rbac3-admin-web/src/features");
        String featureApi = Files.readString(
                webSource.resolve("shared/FeatureApi.tsx"));
        String tenantApi = Files.readString(
                webSource.resolve("tenant/tenant.api.ts"));
        assertTrue(featureApi.contains("'" + TenantContextResolver.TARGET_HEADER + "'"));
        assertTrue(tenantApi.contains("/api/rbac3/v1/platform/tenants/"));
    }

    @Test
    void tenantDetailEndpointIsExplicitlyPlatformScoped() {
        boolean found = Arrays.stream(TenantController.class
                        .getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .map(method -> method.getAnnotation(GetMapping.class))
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch("/platform/tenants/{tenantId}"::equals);

        assertTrue(found);
    }

    private static CurrentRbac3Principal principal() {
        return new CurrentRbac3Principal(
                "20001", "10001", "40001", 3L, 5L, 7L,
                Set.of("system:tenant:target"), true);
    }
}
