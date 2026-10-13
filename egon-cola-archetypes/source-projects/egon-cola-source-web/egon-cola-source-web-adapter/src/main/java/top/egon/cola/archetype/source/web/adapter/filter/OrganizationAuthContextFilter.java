package top.egon.cola.archetype.source.web.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;

/** Binds business context after the host's verified USER boundary; ingress headers only carry correlation IDs. */
@Component("organizationAuthContextFilter")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class OrganizationAuthContextFilter extends OncePerRequestFilter {

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Value("${egon.cola.component.mybatis-plus.tenant-id.mdc-key:tenantId}")
    private final String tenantMdcKey;
    @Value("${egon.cola.component.mybatis-plus.audit.user-id-mdc-key:userId}")
    private final String actorMdcKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/actuator/health/liveness") || path.equals("/actuator/health/readiness");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        IdentityPrincipal identity = null;
        if (authentication instanceof Rbac3AuthenticationToken token && token.isAuthenticated()) {
            identity = token.getPrincipal().identity();
        } else if (authentication instanceof IdpAuthenticationToken token && token.isAuthenticated()
                && token.getPrincipal() instanceof IdentityPrincipal user) {
            identity = user;
        }
        if (identity == null) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Tianquan-Shoubing authentication failed");
            return;
        }
        String actor = identity.subject();
        Long tenant;
        try {
            if (identity.tenantId() == null || !identity.tenantId().matches("[1-9][0-9]{0,18}")
                    || actor == null || actor.isBlank() || actor.length() > 128 || !actor.equals(actor.strip())) {
                throw new IllegalArgumentException("Invalid verified business identity");
            }
            tenant = Long.valueOf(identity.tenantId());
        } catch (IllegalArgumentException invalid) {
            writeFailure(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AUTHORIZATION_RUNTIME_UNAVAILABLE", "Tianquan-Jianshen authorization runtime unavailable");
            return;
        }
        var previousContext = OrganizationRequestContextHolder.current();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) { traceId = UUID.randomUUID().toString(); }
        try {
            MDC.put("tenantId", tenant.toString());
            MDC.put(tenantMdcKey, tenant.toString());
            MDC.put("userId", actor);
            MDC.put(actorMdcKey, actor);
            MDC.put("egon.auth.actor-kind", "USER");
            OrganizationRequestContextHolder.clear();
            OrganizationRequestContextHolder.set(new OrganizationRequestContext(actor, Set.of(), traceId));
            filterChain.doFilter(request, response);
        } finally {
            OrganizationRequestContextHolder.clear();
            previousContext.ifPresent(OrganizationRequestContextHolder::set);
            MDC.clear();
            if (previousMdc != null && !previousMdc.isEmpty()) { MDC.setContextMap(previousMdc); }
        }
    }

    private void writeFailure(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        if (status == HttpServletResponse.SC_UNAUTHORIZED) { response.setHeader("WWW-Authenticate", "Bearer"); }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("code", code, "message", message));
    }

}
