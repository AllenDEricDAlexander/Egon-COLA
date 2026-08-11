package top.egon.cola.platform.rbac3.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 使用本系统的 RBAC3 快照增强 IdP 已认证的用户请求，服务请求保持 IdP 身份不变。
 * Enriches an IdP-authenticated user request with this system's RBAC3 snapshot while leaving
 * service requests under the IdP identity.
 */
public final class Rbac3BearerAuthenticationFilter extends OncePerRequestFilter {

    private final SingleFlightSnapshotLoader snapshotLoader;
    private final ObjectMapper objectMapper;

    public Rbac3BearerAuthenticationFilter(
            SingleFlightSnapshotLoader snapshotLoader,
            ObjectMapper objectMapper
    ) {
        this.snapshotLoader = Objects.requireNonNull(snapshotLoader, "snapshotLoader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (!(authentication instanceof IdpAuthenticationToken idp)
                || !(idp.getPrincipal() instanceof IdentityPrincipal user)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthorizationService.RuntimeAuthorizationContext context =
                    new AuthorizationService.RuntimeAuthorizationContext(
                            user, snapshotLoader.load(user), false);
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new Rbac3AuthenticationToken(context));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (Rbac3AuthorizationClient.AuthorizationDeniedException exception) {
            failure(response, HttpServletResponse.SC_FORBIDDEN,
                    exception.getMessage());
        } catch (Rbac3AuthorizationClient.AuthorizationUnavailableException exception) {
            failure(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void failure(
            HttpServletResponse response,
            int status,
            String reasonCode)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "RBAC3 authorization context is unavailable"));
    }
}
