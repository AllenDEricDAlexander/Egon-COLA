package top.egon.cola.platform.rbac3.admin.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.tenant.controller.filter.TenantContextFilter;
import top.egon.cola.platform.rbac3.admin.tenant.service.TenantContextResolver;

class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter(
            new TenantContextResolver());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void rejectsMissingTenantAndOrdinaryTargetTenantForgery() throws Exception {
        MockHttpServletResponse missing = execute(request("/api/v1/auth/bootstrap"));
        assertEquals(401, missing.getStatus());

        authenticate(principal("tenant-1", Set.of()));
        MockHttpServletRequest forged = request("/api/v1/auth/bootstrap");
        forged.addHeader("X-RBAC3-Target-Tenant", "tenant-2");
        assertEquals(403, execute(forged).getStatus());
    }

    @Test
    void rejectsTenantConflictBeforeTheRequestChain() throws Exception {
        authenticate(principal("tenant-1", Set.of()));
        MockHttpServletRequest request = request("/api/v1/auth/bootstrap");
        request.addHeader("X-RBAC3-Tenant", "tenant-2");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertEquals(null, chain.getRequest());
    }

    @Test
    void allowsExplicitPlatformTargetOnlyWithPermission() throws Exception {
        authenticate(new CurrentRbac3Principal(
                "platform", "user-1", "session-1", 1, 1, 1,
                Set.of("system:tenant:target"), true));
        MockHttpServletRequest request = request("/api/v1/platform/tenants/users");
        request.addHeader("X-RBAC3-Target-Tenant", "tenant-2");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("tenant-2", request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
    }

    @Test
    void acceptsServicePrincipalOnlyForItsCredentialTenant() throws Exception {
        Instant issuedAt = Instant.parse("2026-08-10T00:00:00Z");
        ServiceIdentityPrincipal principal = new ServiceIdentityPrincipal(
                "finance-service", "tenant-1", "finance-service",
                "service-token-1",
                URI.create("https://api.example/prod/permission/rbac3"),
                12L, Set.of("service:authorization:decide"),
                "finance", "finance-web", "prod", "credential-1",
                issuedAt, issuedAt.plusSeconds(300));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, "n/a", Set.of()));
        MockHttpServletRequest request = request(
                "/api/rbac3/v1/internal/authorization/decisions");
        request.addHeader("X-RBAC3-Tenant", "tenant-1");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("tenant-1",
                request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private void authenticate(CurrentRbac3Principal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, "n/a", principal.authorities()));
    }

    private CurrentRbac3Principal principal(String tenantId, Set<String> permissions) {
        return new CurrentRbac3Principal(
                tenantId, "user-1", "session-1", 1, 1, 1,
                permissions, false);
    }
}
