package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Retires only the ten legacy root protocol operations without inspecting credentials. */
@Component("legacyOAuthRetirementFilter")
@Slf4j
@RequiredArgsConstructor
public class LegacyOAuthRetirementFilter extends OncePerRequestFilter {

    private static final Map<String, String> RETIRED_OPERATIONS = Map.of(
            "/.well-known/oauth-authorization-server", "GET",
            "/oauth2/jwks", "GET", "/oauth2/login/csrf", "GET", "/oauth2/login", "POST",
            "/oauth2/token", "POST", "/oauth2/revoke", "POST", "/oauth2/logout", "POST",
            "/oauth2/step-up", "POST", "/oauth2/userinfo", "GET",
            "/internal/v1/oauth2/refresh-token/validate", "POST");

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !request.getMethod().equals(RETIRED_OPERATIONS.get(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_GONE);
        response.setContentType("application/json");
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", "legacy_endpoint_retired",
                "error_description", "Use the configured issuer authorization endpoints."));
    }
}
