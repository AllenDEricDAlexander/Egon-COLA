package top.egon.cola.component.yuheng.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.springframework.security.core.GrantedAuthority;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * Converts Tianquan-Shoubing-signed Gateway Admin Service scopes into local method-security capabilities.
 *
 * <p>The control-plane publisher uses a short-lived Tianquan-Shoubing SERVICE Access Token. The token remains
 * the source of truth; this filter only adapts its already verified {@code yuheng:*} scopes to
 * the existing {@code CAP_...} method-security vocabulary. USER identities are left to the
 * Tianquan-Jianshen snapshot filter.</p>
 */
@Slf4j
@RequiredArgsConstructor
public final class GatewayAdminServiceAuthorityFilter
        extends OncePerRequestFilter {

    /**
     * Prefix used by Gateway Admin method-security expressions.
     */
    private static final String CAPABILITY_PREFIX = "CAP_";

    /**
     * Only Tianquan-Shoubing scopes owned by the Gateway Admin resource become local capabilities.
     */
    private static final String YUHENG_SCOPE_PREFIX = "yuheng:";

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
            List<GrantedAuthority> authorities = new ArrayList<>(authentication.getAuthorities().stream()
                    .filter(authority -> authority.getAuthority() != null && authority.getAuthority().startsWith("SCOPE_")).toList());
            authorities.addAll(service.scopes().stream()
                    .filter(scope -> scope.startsWith(YUHENG_SCOPE_PREFIX))
                    .sorted()
                    .map(scope -> new SimpleGrantedAuthority(
                            CAPABILITY_PREFIX + scope))
                    .toList());
            var replacement = new UsernamePasswordAuthenticationToken(
                            service,
                            "",
                            authorities);
            replacement.setDetails(authentication.getDetails());
            SecurityContextHolder.getContext().setAuthentication(replacement);
        }
        filterChain.doFilter(request, response);
    }
}
