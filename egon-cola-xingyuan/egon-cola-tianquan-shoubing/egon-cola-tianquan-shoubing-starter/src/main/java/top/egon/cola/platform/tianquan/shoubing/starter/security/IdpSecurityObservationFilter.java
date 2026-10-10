package top.egon.cola.platform.tianquan.shoubing.starter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

/** Request-scoped pre-render audit bridge; it is installed only in the host security chain. */
@Slf4j
@RequiredArgsConstructor
public class IdpSecurityObservationFilter extends OncePerRequestFilter {
    @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;
    @Qualifier("idpEndpointAuthenticationPolicy") private final IdpEndpointAuthenticationPolicy endpointPolicy;

    private static final String AUTH_CALLBACK = IdpSecurityObservationFilter.class.getName() + ".authCallback";
    private static final String DENIED_CALLBACK = IdpSecurityObservationFilter.class.getName() + ".deniedCallback";
    private static final String AUTH_DONE = IdpSecurityObservationFilter.class.getName() + ".authDone";
    private static final String DENIED_DONE = IdpSecurityObservationFilter.class.getName() + ".deniedDone";

    public static boolean beforeAuthenticationFailure(HttpServletRequest request) {
        return beforeRender(request, AUTH_CALLBACK, AUTH_DONE);
    }

    public static boolean beforeAuthorizationDenied(HttpServletRequest request) {
        return beforeRender(request, DENIED_CALLBACK, DENIED_DONE);
    }

    /** Lets the owning PDP renderer avoid repeating a decision that was already handed off. */
    public static void authorizationDeniedRecorded(HttpServletRequest request) {
        request.setAttribute(DENIED_DONE, Boolean.TRUE);
    }

    private static boolean beforeRender(HttpServletRequest request, String callbackKey, String doneKey) {
        if (!(request.getAttribute(callbackKey) instanceof BooleanSupplier callback)) { return true; }
        if (request.getAttribute(doneKey) instanceof Boolean completed) { return completed; }
        boolean completed = callback.getAsBoolean();
        request.setAttribute(doneKey, completed);
        return completed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!observations.enabled() || endpointPolicy.requirement(request) == IdpEndpointAuthenticationPolicy.Requirement.PUBLIC
                || "OPTIONS".equals(request.getMethod()) && request.getHeader("Origin") != null
                        && request.getHeader("Access-Control-Request-Method") != null) {
            chain.doFilter(request, response);
            return;
        }
        String previousTrace = MDC.get("traceId");
        if (previousTrace == null || !previousTrace.matches("[A-Za-z0-9._~-]{1,128}")) {
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
        }
        request.setAttribute(AUTH_CALLBACK, (BooleanSupplier) observations::authenticationFailure);
        request.setAttribute(DENIED_CALLBACK, (BooleanSupplier) () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication == null || !(authentication.getDetails() instanceof TokenAuditProofBO proof)
                    || observations.authorizationDenied(proof);
        });
        try {
            chain.doFilter(request, response);
        } finally {
            request.removeAttribute(AUTH_CALLBACK);
            request.removeAttribute(DENIED_CALLBACK);
            request.removeAttribute(AUTH_DONE);
            request.removeAttribute(DENIED_DONE);
            if (previousTrace == null) { MDC.remove("traceId"); } else { MDC.put("traceId", previousTrace); }
        }
    }
}
