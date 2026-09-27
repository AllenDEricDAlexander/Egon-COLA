package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
import top.egon.cola.component.tianshu.admin.security.management.DdcAdminAuthenticationEntryPoint;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Slf4j
@RequiredArgsConstructor
public class DdcTenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Egon-Tianshu-Tenant";

    @Qualifier("ddcTenantRegistry")
    private final DdcTenantRegistry tenantRegistry;

    @Qualifier("ddcTenantContextExecutor")
    private final DdcTenantContextExecutor tenantContextExecutor;

    @Qualifier("ddcAdminAuthenticationEntryPoint")
    private final DdcAdminAuthenticationEntryPoint securityHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.startsWith("/actuator/")) {
            return true;
        }
        return !path.startsWith("/api/v1/tianshu/")
                && !path.equals("/api/v1/auth/bootstrap")
                && !path.equals("/api/v1/tianshu/auth/bootstrap");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!(authentication.getPrincipal() instanceof Rbac3UserDetails user)) {
            securityHandler.handleTenantMismatch(request, response);
            return;
        }

        DdcTenantContextDTO tenantContext;
        try {
            tenantContext = tenantRegistry.resolveUserContext(
                    user.tenantId(), user.identitySub());
            if (!matchesExpectedTenant(request, user.tenantId())) {
                securityHandler.handleTenantMismatch(request, response);
                return;
            }
        } catch (IllegalArgumentException invalidTenant) {
            securityHandler.handleTenantMismatch(request, response);
            return;
        }

        try (DdcTenantContextExecutor.Scope ignored = tenantContextExecutor.bind(tenantContext)) {
            filterChain.doFilter(request, response);
        }
    }

    private boolean matchesExpectedTenant(HttpServletRequest request, String identityTenantId) {
        Enumeration<String> values = request.getHeaders(TENANT_HEADER);
        if (values == null || !values.hasMoreElements()) {
            return !isAiPath(request.getRequestURI());
        }
        String expectedHeader = values.nextElement();
        if (values.hasMoreElements() || expectedHeader == null || expectedHeader.isBlank()) {
            return false;
        }
        String decoded;
        try {
            decoded = UriUtils.decode(expectedHeader, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalidEncoding) {
            return false;
        }
        return !hasControlCharacter(decoded) && decoded.equals(identityTenantId);
    }

    private boolean isAiPath(String path) {
        return path != null && path.startsWith("/api/v1/tianshu/ai/");
    }

    private boolean hasControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }
}
