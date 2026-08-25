package top.egon.cola.platform.rbac3.admin.shared.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.admin.shared.tenant.controller.filter.TenantContextFilter;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.admin.shared.tenant.service.TenantContextResolver;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        authenticate(principal("platform", Set.of("system:tenant:target")));
        MockHttpServletRequest request = request("/api/v1/platform/tenants/users");
        request.addHeader("X-RBAC3-Target-Tenant", "tenant-2");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("tenant-2", request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
        assertEquals(TenantContext.class.getName(), TenantContextFilter.TENANT_ATTRIBUTE);
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

    private void authenticate(Rbac3UserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, "n/a", principal.getAuthorities()));
    }

    private Rbac3UserDetails principal(String tenantId, Set<String> permissions) {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "user-1", tenantId, "access-token", Set.of("rbac3-admin-web"),
                now, now.plusSeconds(300), AuthenticationContext.of("TEST", now));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                tenantId, "user-1", "user-1", "rbac3-admin", 1L, 1L,
                List.of(), permissions, Map.of(), Map.of(),
                "sha256:test", now, now.plusSeconds(300));
        return new Rbac3UserDetails(identity, snapshot);
    }
}
