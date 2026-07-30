package top.egon.cola.platform.rbac3.admin.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class TenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_ATTRIBUTE = TenantContext.class.getName();
    private final TenantContextResolver resolver;

    public TenantContextFilter(TenantContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            TenantContext context = resolver.resolve(
                    request, SecurityContextHolder.getContext().getAuthentication());
            TenantContext.set(context);
            request.setAttribute(TENANT_ATTRIBUTE, context.effectiveTenantId());
            filterChain.doFilter(request, response);
        } catch (TenantContextResolver.TenantContextResolutionException error) {
            response.setStatus(error.status());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":{\"code\":\""
                    + error.reasonCode() + "\"}}");
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health/")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh");
    }
}
