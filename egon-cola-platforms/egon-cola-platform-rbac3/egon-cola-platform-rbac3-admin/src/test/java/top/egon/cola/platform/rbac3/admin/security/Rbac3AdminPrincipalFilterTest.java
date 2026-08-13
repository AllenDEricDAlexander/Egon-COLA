package top.egon.cola.platform.rbac3.admin.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3AuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.Rbac3AdminAuthenticationToken;
import top.egon.cola.platform.rbac3.admin.config.security.Rbac3AdminPrincipalFilter;

class Rbac3AdminPrincipalFilterTest {

    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void projectsStarterContextWithoutReadingAuthorizationClaims() throws Exception {
        IdentityPrincipal identity = new IdentityPrincipal(
                "alice-sub", "tenant-a", "sid-1", "rbac3-admin-web",
                "token-1", 2, Set.of("rbac3-admin-web"),
                NOW, NOW.plusSeconds(900));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "alice-sub", "101", "sid-1", "rbac3-admin",
                7, 8, 9, List.of("role-1"),
                Set.of("system:bootstrap:read", "system:platform:admin"),
                Map.of(), Map.of(), "sha256:rbac3", NOW, NOW.plusSeconds(900));
        var runtime = new AuthorizationService.RuntimeAuthorizationContext(
                identity, snapshot, false);
        SecurityContextHolder.getContext().setAuthentication(
                new Rbac3AuthenticationToken(runtime));
        AtomicReference<Object> principal = new AtomicReference<>();

        new Rbac3AdminPrincipalFilter().doFilter(
                new MockHttpServletRequest("GET", "/api/v1/auth/bootstrap"),
                new MockHttpServletResponse(),
                (request, response) -> principal.set(
                        SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal()));

        assertThat(principal.get()).isEqualTo(new CurrentRbac3Principal(
                "tenant-a", "alice-sub", "101", "sid-1",
                7, 8, 9,
                Set.of("system:bootstrap:read", "system:platform:admin"), true));
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(Rbac3AdminAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice-sub");
    }
}
