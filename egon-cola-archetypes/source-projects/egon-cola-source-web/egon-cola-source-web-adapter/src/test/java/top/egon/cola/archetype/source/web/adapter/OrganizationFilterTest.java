package top.egon.cola.archetype.source.web.adapter;

import top.egon.cola.archetype.source.web.adapter.filter.OrganizationAuthContextFilter;
import top.egon.cola.archetype.source.web.adapter.filter.OrganizationTraceFilter;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityContractTest.SecurityFixture;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationFilterTest {
    @Test
    void bindsVerifiedActorIgnoresHeaderRolesAndAlwaysClearsContext(@TempDir Path temporary) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-1");
        request.addHeader("X-Actor-Id", "admin-1");
        request.addHeader("X-Actor-Roles", "ORGANIZATION_ADMIN, TEACHING_ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean rolesObserved = new AtomicBoolean();
        FilterChain terminal = (req, res) -> {
            rolesObserved.set(OrganizationRequestContextHolder.current()
                .map(context -> "subject-1001".equals(context.actorId()) && context.actorRoles().isEmpty()).orElse(false));
            throw new IllegalStateException("boom");
        };
        OrganizationTraceFilter trace = new OrganizationTraceFilter();
        try (var security = new SecurityFixture(temporary)) {
            SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of("openid")));
            var auth = new OrganizationAuthContextFilter(security.mapper, "tenantId", "userId");
            assertThrows(IllegalStateException.class,
                () -> trace.doFilter(request, response, (req, res) -> auth.doFilter(req, res, terminal)));
        }

        assertTrue(rolesObserved.get());
        assertFalse(OrganizationRequestContextHolder.current().isPresent());
        assertTrue("trace-1".equals(response.getHeader("X-Trace-Id")));
    }
}
