package top.egon.cola.platform.tianquan.shoubing.starter.security;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
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
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
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
    private static final String AUTH_STATE = IdpSecurityObservationFilter.class.getName() + ".authState";
    private static final String DENIED_STATE = IdpSecurityObservationFilter.class.getName() + ".authorizationObservationContext";
    private static final String ASYNC_LISTENER = IdpSecurityObservationFilter.class.getName() + ".asyncListener";

    public static boolean beforeAuthenticationFailure(HttpServletRequest request) {
        return beforeRender(request, AUTH_CALLBACK, AUTH_DONE);
    }

    public static boolean beforeAuthorizationDenied(HttpServletRequest request) {
        return beforeRender(request, DENIED_CALLBACK, DENIED_DONE);
    }

    public static IdpAuthorizationObservationContextBO authorizationObservationContext(HttpServletRequest request) {
        return request.getAttribute(DENIED_STATE) instanceof IdpAuthorizationObservationContextBO context ? context : null;
    }

    /** The owning PDP calls this only after a successful strict handoff. */
    public static void authorizationDeniedRecorded(HttpServletRequest request) {
        var context = authorizationObservationContext(request);
        if (context != null && context.isConfirmed()) { request.setAttribute(DENIED_DONE, Boolean.TRUE); }
    }

    private static boolean beforeRender(HttpServletRequest request, String callbackKey, String doneKey) {
        if (request.getAttribute(doneKey) instanceof Boolean completed) { return completed; }
        if (!(request.getAttribute(callbackKey) instanceof BooleanSupplier callback)) { return true; }
        boolean completed = callback.getAsBoolean();
        request.setAttribute(doneKey, completed);
        return completed;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }

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
        if (authorizationObservationContext(request) == null) { installCallbacks(request, MDC.get("traceId")); }
        Throwable primary = null;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                if (request.isAsyncStarted()) { retainForAsyncCompletion(request); }
                else { closeObservation(request); }
            } catch (ServletException lifecycleFailure) {
                if (primary == null) { throw lifecycleFailure; }
                primary.addSuppressed(lifecycleFailure);
            } finally {
                if (previousTrace == null) { MDC.remove("traceId"); } else { MDC.put("traceId", previousTrace); }
            }
        }
    }

    private void installCallbacks(HttpServletRequest request, String trace) {
        var authenticationState = new IdpAuthorizationObservationContextBO();
        var deniedState = new IdpAuthorizationObservationContextBO();
        request.setAttribute(AUTH_STATE, authenticationState);
        request.setAttribute(DENIED_STATE, deniedState);
        request.setAttribute(AUTH_CALLBACK, (BooleanSupplier) () -> authenticationState.denyOnce(() -> {
            String previous = MDC.get("traceId");
            try { MDC.put("traceId", trace); return observations.authenticationFailure(); }
            finally { if (previous == null) { MDC.remove("traceId"); } else { MDC.put("traceId", previous); } }
        }));
        request.setAttribute(DENIED_CALLBACK, (BooleanSupplier) () -> deniedState.denyOnce(() -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.getDetails() instanceof TokenAuditProofBO proof
                    && observations.authorizationDenied(proof, trace);
        }));
    }

    private void retainForAsyncCompletion(HttpServletRequest request) throws ServletException {
        if (request.getAttribute(ASYNC_LISTENER) != null) { return; }
        AsyncListener listener = new AuthorizationObservationAsyncListener(request);
        request.setAttribute(ASYNC_LISTENER, listener);
        try { request.getAsyncContext().addListener(listener); }
        catch (IllegalStateException unavailable) {
            if (!request.isAsyncStarted()) { closeObservation(request); }
            else {
                request.removeAttribute(ASYNC_LISTENER);
                throw new ServletException("OAUTH_AUDIT_ASYNC_LIFECYCLE_UNAVAILABLE", unavailable);
            }
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    private static class AuthorizationObservationAsyncListener implements AsyncListener {
        private final HttpServletRequest request;

        @Override public void onComplete(AsyncEvent event) { closeObservation(request); }
        @Override public void onError(AsyncEvent event) { closeObservation(request); }
        @Override public void onTimeout(AsyncEvent event) { closeObservation(request); }
        @Override public void onStartAsync(AsyncEvent event) throws IOException {
            try { event.getAsyncContext().addListener(this); }
            catch (IllegalStateException unavailable) {
                if (!request.isAsyncStarted()) { closeObservation(request); }
                else { throw new IOException("OAUTH_AUDIT_ASYNC_LIFECYCLE_UNAVAILABLE", unavailable); }
            }
        }
    }

    private static void closeObservation(HttpServletRequest request) {
        if (request.getAttribute(AUTH_STATE) instanceof IdpAuthorizationObservationContextBO state) { state.close(); }
        if (request.getAttribute(DENIED_STATE) instanceof IdpAuthorizationObservationContextBO state) { state.close(); }
        for (String key : new String[] { AUTH_CALLBACK, DENIED_CALLBACK, AUTH_DONE, DENIED_DONE, AUTH_STATE, DENIED_STATE, ASYNC_LISTENER }) {
            request.removeAttribute(key);
        }
    }
}
