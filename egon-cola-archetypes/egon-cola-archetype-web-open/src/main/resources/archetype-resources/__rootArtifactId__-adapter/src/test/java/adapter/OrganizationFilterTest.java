package ${package}.adapter;

import ${package}.adapter.filter.OrganizationAuthContextFilter;
import ${package}.adapter.filter.OrganizationTraceFilter;
import ${package}.application.context.OrganizationRequestContextHolder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationFilterTest {
    @Test
    void propagatesActorRolesAndAlwaysClearsContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-1");
        request.addHeader("X-Actor-Id", "admin-1");
        request.addHeader("X-Actor-Roles", "ORGANIZATION_ADMIN, TEACHING_ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean rolesObserved = new AtomicBoolean();
        FilterChain terminal = (req, res) -> {
            rolesObserved.set(OrganizationRequestContextHolder.current()
                .map(context -> context.hasRole("TEACHING_ADMIN")).orElse(false));
            throw new IllegalStateException("boom");
        };
        OrganizationAuthContextFilter auth = new OrganizationAuthContextFilter();
        OrganizationTraceFilter trace = new OrganizationTraceFilter();

        assertThrows(IllegalStateException.class,
            () -> trace.doFilter(request, response, (req, res) -> auth.doFilter(req, res, terminal)));

        assertTrue(rolesObserved.get());
        assertFalse(OrganizationRequestContextHolder.current().isPresent());
        assertTrue("trace-1".equals(response.getHeader("X-Trace-Id")));
    }
}
