package top.egon.cola.platform.rbac3.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeSnapshotReader;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Accepts one Authorization Bearer credential and rejects all ambiguous forms.
 */
public final class Rbac3BearerAuthenticationFilter extends OncePerRequestFilter {

    private final Rbac3JwtVerifier jwtVerifier;
    private final Rbac3RuntimeSnapshotReader snapshotReader;
    private final ObjectMapper objectMapper;

    public Rbac3BearerAuthenticationFilter(
            Rbac3JwtVerifier jwtVerifier,
            Rbac3RuntimeSnapshotReader snapshotReader,
            ObjectMapper objectMapper
    ) {
        this.jwtVerifier = Objects.requireNonNull(jwtVerifier, "jwtVerifier");
        this.snapshotReader = Objects.requireNonNull(snapshotReader, "snapshotReader");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (headers.size() != 1 || !headers.getFirst().startsWith("Bearer ")) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        String token = headers.getFirst().substring("Bearer ".length()).trim();
        if (token.isEmpty() || token.contains(",")) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        try {
            var claims = jwtVerifier.verify(token);
            AuthorizationService.RuntimeAuthorizationContext context =
                    snapshotReader.read(claims);
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new Rbac3AuthenticationToken(context));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (Rbac3JwtVerifier.InvalidTokenException exception) {
            unauthorized(response, "JWT_INVALID");
        } catch (AuthorizationService.RuntimeUnavailableException exception) {
            unauthorized(response, exception.reasonCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void unauthorized(HttpServletResponse response, String reasonCode)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "RBAC3 authentication failed"));
    }
}
