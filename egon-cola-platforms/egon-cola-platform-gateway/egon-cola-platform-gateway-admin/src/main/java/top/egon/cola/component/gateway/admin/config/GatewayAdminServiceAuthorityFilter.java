package top.egon.cola.component.gateway.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpAuthenticationToken;

import java.io.IOException;
import java.util.List;

/**
 * Converts IdP-signed Gateway Admin Service scopes into local method-security capabilities.
 *
 * <p>The control-plane publisher uses a short-lived IdP SERVICE Access Token. The token remains
 * the source of truth; this filter only adapts its already verified {@code gateway:*} scopes to
 * the existing {@code CAP_...} method-security vocabulary. USER identities are left to the
 * RBAC3 snapshot filter.</p>
 */
public final class GatewayAdminServiceAuthorityFilter
        extends OncePerRequestFilter {

    /**
     * Prefix used by Gateway Admin method-security expressions.
     */
    private static final String CAPABILITY_PREFIX = "CAP_";

    /**
     * Only IdP scopes owned by the Gateway Admin resource become local capabilities.
     */
    private static final String GATEWAY_SCOPE_PREFIX = "gateway:";

    /**
     * Adapts one verified SERVICE identity for the Gateway Admin control plane.
     *
     * @param request     current HTTP request
     * @param response    current HTTP response
     * @param filterChain downstream filter chain
     * @throws ServletException when downstream processing fails
     * @throws IOException      when downstream processing fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication instanceof IdpAuthenticationToken idp
                && idp.getPrincipal() instanceof ServiceIdentityPrincipal service) {
            List<SimpleGrantedAuthority> authorities = service.scopes().stream()
                    .filter(scope -> scope.startsWith(GATEWAY_SCOPE_PREFIX))
                    .sorted()
                    .map(scope -> new SimpleGrantedAuthority(
                            CAPABILITY_PREFIX + scope))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            service,
                            "",
                            authorities));
        }
        filterChain.doFilter(request, response);
    }
}
