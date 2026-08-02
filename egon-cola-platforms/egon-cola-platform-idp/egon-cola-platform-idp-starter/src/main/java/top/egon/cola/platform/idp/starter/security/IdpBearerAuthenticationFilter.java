package top.egon.cola.platform.idp.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Establishes an identity-only Spring Security context from one Bearer token.
 */
public final class IdpBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final int MAX_CREDENTIAL_LENGTH = 8192;

    private final IdpJwtVerifier jwtVerifier;
    private final ObjectMapper objectMapper;

    public IdpBearerAuthenticationFilter(
            IdpJwtVerifier jwtVerifier,
            ObjectMapper objectMapper
    ) {
        this.jwtVerifier = Objects.requireNonNull(jwtVerifier, "jwtVerifier");
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
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (headers.size() != 1 || !isBearer(headers.getFirst())) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        String token = headers.getFirst().substring("Bearer ".length()).trim();
        if (token.isEmpty() || token.contains(",")
                || token.length() > MAX_CREDENTIAL_LENGTH) {
            unauthorized(response, "AUTHORIZATION_HEADER_INVALID");
            return;
        }
        try {
            var principal = jwtVerifier.verify(token);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new IdpAuthenticationToken(principal));
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (IdpJwtVerifier.InvalidTokenException exception) {
            unauthorized(response, exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isBearer(String header) {
        return header != null
                && header.length() >= "Bearer ".length()
                && header.regionMatches(
                        true, 0, "Bearer ", 0, "Bearer ".length());
    }

    private void unauthorized(HttpServletResponse response, String reasonCode)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", reasonCode,
                "message", "IdP authentication failed"));
    }
}
