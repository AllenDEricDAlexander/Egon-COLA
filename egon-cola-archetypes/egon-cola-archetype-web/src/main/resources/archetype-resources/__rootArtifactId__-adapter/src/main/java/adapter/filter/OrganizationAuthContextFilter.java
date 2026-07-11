package ${package}.adapter.filter;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component("organizationAuthContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class OrganizationAuthContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String actorId = defaultValue(request.getHeader("X-Actor-Id"), "anonymous");
        Set<String> roles = parseRoles(request.getHeader("X-Actor-Roles"));
        String traceId = defaultValue(MDC.get("traceId"), "unknown");
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(actorId, roles, traceId));
        try {
            filterChain.doFilter(request, response);
        } finally {
            OrganizationRequestContextHolder.clear();
        }
    }

    private static Set<String> parseRoles(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(",")).map(String::trim).filter(value -> !value.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
