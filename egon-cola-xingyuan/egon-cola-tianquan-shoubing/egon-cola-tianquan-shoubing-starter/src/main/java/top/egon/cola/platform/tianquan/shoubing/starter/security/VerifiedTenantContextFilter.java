package top.egon.cola.platform.tianquan.shoubing.starter.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

/** Scopes verified tenant/user MDC values around RBAC and the downstream servlet chain. */
@Slf4j
@RequiredArgsConstructor
public final class VerifiedTenantContextFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)
                || !(SecurityContextHolder.getContext().getAuthentication()
                instanceof IdpAuthenticationToken authentication)) {
            chain.doFilter(request, response);
            return;
        }
        IdpPrincipal principal = authentication.getPrincipal();
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            bind(principal);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
            if (previous != null && !previous.isEmpty()) {
                MDC.setContextMap(previous);
            }
        }
    }

    private void bind(IdpPrincipal principal) {
        String tenantId = principal.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            MDC.remove("tenantId");
        } else {
            MDC.put("tenantId", tenantId);
        }
        if (principal instanceof IdentityPrincipal user) {
            MDC.put("userId", user.subject());
        } else {
            MDC.remove("userId");
        }
    }
}
