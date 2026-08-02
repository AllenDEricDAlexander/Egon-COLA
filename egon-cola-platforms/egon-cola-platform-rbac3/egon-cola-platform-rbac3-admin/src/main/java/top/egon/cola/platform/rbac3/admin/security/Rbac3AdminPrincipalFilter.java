package top.egon.cola.platform.rbac3.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3ContextAuthentication;

import java.io.IOException;

/** Projects the generic Starter context into RBAC3 Admin's established principal. */
public final class Rbac3AdminPrincipalFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof Rbac3ContextAuthentication source
                && !(authentication instanceof Rbac3AdminAuthenticationToken)) {
            var context = source.context();
            var snapshot = context.snapshot();
            var principal = new CurrentRbac3Principal(
                    snapshot.tenantId(), snapshot.identitySub(),
                    snapshot.rbac3UserId(), snapshot.sessionId(),
                    snapshot.authVersion(), snapshot.contextVersion(),
                    snapshot.policyVersion(), snapshot.permissions(),
                    snapshot.permissions().contains("system:platform:admin"));
            var projected = SecurityContextHolder.createEmptyContext();
            projected.setAuthentication(new Rbac3AdminAuthenticationToken(
                    principal, context));
            SecurityContextHolder.setContext(projected);
        }
        filterChain.doFilter(request, response);
    }
}
