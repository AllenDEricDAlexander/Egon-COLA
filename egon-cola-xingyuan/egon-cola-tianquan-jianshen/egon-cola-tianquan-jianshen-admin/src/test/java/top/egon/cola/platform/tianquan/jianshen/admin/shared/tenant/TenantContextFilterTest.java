package top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.controller.filter.TenantContextFilter;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.service.TenantContextResolver;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        forged.addHeader("X-TIANQUAN-JIANSHEN-Target-Tenant", "tenant-2");
        assertEquals(403, execute(forged).getStatus());
    }

    @Test
    void rejectsTenantConflictBeforeTheRequestChain() throws Exception {
        authenticate(principal("tenant-1", Set.of()));
        MockHttpServletRequest request = request("/api/v1/auth/bootstrap");
        request.addHeader("X-TIANQUAN-JIANSHEN-Tenant", "tenant-2");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertEquals(null, chain.getRequest());
    }

    @Test
    void allowsExplicitPlatformTargetOnlyWithPermission() throws Exception {
        authenticate(principal("platform", Set.of("system:platform:admin", "system:tenant:target")));
        MockHttpServletRequest request = request("/api/v1/platform/tenants/users");
        request.addHeader("X-TIANQUAN-JIANSHEN-Target-Tenant", "tenant-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
            assertEquals("tenant-2",
                    currentRequest.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
        });

        assertEquals(200, response.getStatus());
        assertNull(request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
        assertEquals(TenantContext.class.getName(), TenantContextFilter.TENANT_ATTRIBUTE);
    }

    @Test
    void targetTenantPermissionAloneDoesNotImplyPlatformAdministration() throws Exception {
        authenticate(principal("platform", Set.of("system:tenant:target")));
        MockHttpServletRequest request = request("/api/tianquan-jianshen/v1/platform/tenants/users");
        request.addHeader("X-TIANQUAN-JIANSHEN-Target-Tenant", "tenant-2");
        assertEquals(403, execute(request).getStatus());
    }

    @Test
    void acceptsServicePrincipalOnlyForItsCredentialTenant() throws Exception {
        Instant issuedAt = Instant.parse("2026-08-10T00:00:00Z");
        ServiceIdentityPrincipal principal = new ServiceIdentityPrincipal(
                "finance-service", "tenant-1", "finance-service",
                "service-token-1",
                URI.create("https://api.example/prod/permission/tianquan-jianshen"),
                12L, Set.of("service:authorization:decide"),
                "finance", "finance-web", "prod", "credential-1",
                issuedAt, issuedAt.plusSeconds(300));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, "n/a", Set.of()));
        MockHttpServletRequest request = request(
                "/api/tianquan-jianshen/v1/internal/authorization/decisions");
        request.addHeader("X-TIANQUAN-JIANSHEN-Tenant", "tenant-1");
        filter.doFilter(request, new MockHttpServletResponse(),
                (currentRequest, response) -> assertEquals("tenant-1",
                        currentRequest.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE)));

        assertNull(request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
    }

    @Test
    void restoresParentContextAndRequestAttributeAfterNestedSuccessAndRejection()
            throws Exception {
        TenantContext parent = new TenantContext("parent-tenant", "parent-tenant", false);
        TenantContext.set(parent);
        authenticate(principal("tenant-1", Set.of()));

        MockHttpServletRequest nestedSuccess = request("/api/v1/auth/bootstrap");
        nestedSuccess.setAttribute(TenantContextFilter.TENANT_ATTRIBUTE, "parent-tenant");
        MockHttpServletResponse successResponse = new MockHttpServletResponse();
        filter.doFilter(nestedSuccess, successResponse, new MockFilterChain());

        assertEquals(200, successResponse.getStatus());
        assertSame(parent, TenantContext.requireCurrent());
        assertEquals("parent-tenant",
                nestedSuccess.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));

        MockHttpServletRequest nestedRejection = request("/api/v1/auth/bootstrap");
        nestedRejection.addHeader("X-TIANQUAN-JIANSHEN-Tenant", "tenant-2");
        nestedRejection.setAttribute(TenantContextFilter.TENANT_ATTRIBUTE, "parent-tenant");
        MockHttpServletResponse rejectionResponse = new MockHttpServletResponse();
        filter.doFilter(nestedRejection, rejectionResponse, new MockFilterChain());

        assertEquals(400, rejectionResponse.getStatus());
        assertSame(parent, TenantContext.requireCurrent());
        assertEquals("parent-tenant",
                nestedRejection.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
    }

    @Test
    void removesTenantRequestAttributeAndThreadContextAfterOuterRequest()
            throws Exception {
        authenticate(principal("tenant-1", Set.of()));
        MockHttpServletRequest request = request("/api/v1/auth/bootstrap");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(request.getAttribute(TenantContextFilter.TENANT_ATTRIBUTE));
        assertThrows(IllegalStateException.class, TenantContext::requireCurrent);
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
                "user-1", tenantId, "access-token", Set.of("tianquan-jianshen-admin-web"),
                now, now.plusSeconds(300), AuthenticationContext.of("PASSWORD", now));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                tenantId, "user-1", "user-1", "tianquan-jianshen-admin", 1L, 1L,
                List.of(), permissions, Map.of(), Map.of(),
                "sha256:test", now, now.plusSeconds(300));
        return new Rbac3UserDetails(identity, snapshot);
    }
}
