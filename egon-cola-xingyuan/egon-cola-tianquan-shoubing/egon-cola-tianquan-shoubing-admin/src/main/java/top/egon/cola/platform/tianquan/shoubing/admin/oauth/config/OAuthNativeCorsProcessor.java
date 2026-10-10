package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;

/** Adds the bounded native preflight contract to Spring's existing CORS processing. */
@Slf4j
@RequiredArgsConstructor
public final class OAuthNativeCorsProcessor implements CorsProcessor {
    private static final Map<String, String> RESOURCES = Map.of(
            "oauth2/token", "POST", "userinfo", "GET",
            ".well-known/openid-configuration", "GET", ".well-known/jwks.json", "GET");
    private static final Set<String> HEADERS = Set.of("authorization", "content-type");

    @Qualifier("oauthDefaultCorsProcessor")
    private final CorsProcessor delegate;

    public static boolean isNativePreflight(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()) && resourceMethod(path(request)) != null;
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static String resourceMethod(String path) {
        if (!path.startsWith("/tenants/")) { return null; }
        int separator = path.indexOf('/', 9);
        return separator < 0 ? null : RESOURCES.get(path.substring(separator + 1));
    }

    @Override
    public boolean processRequest(CorsConfiguration configuration, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isNativePreflight(request)) {
            String path = path(request);
            String tenant = path.substring(9, path.indexOf('/', 9));
            List<String> origins = Collections.list(request.getHeaders("Origin"));
            List<String> methods = Collections.list(request.getHeaders("Access-Control-Request-Method"));
            if (!canonicalTenant(tenant) || origins.size() != 1 || origins.getFirst().length() > 2048
                    || methods.size() != 1 || !resourceMethod(path).equals(methods.getFirst())
                    || configuration == null || configuration.getAllowedOrigins() == null
                    || !configuration.getAllowedOrigins().contains(origins.getFirst())
                    || !validRequestedHeaders(request)) {
                return reject(response);
            }
        }
        return delegate.processRequest(configuration, request, response);
    }

    private static boolean canonicalTenant(String tenant) {
        if (!tenant.matches("[1-9][0-9]{0,18}")) { return false; }
        try { return Long.parseLong(tenant) > 0; }
        catch (NumberFormatException invalid) { return false; }
    }

    private static boolean validRequestedHeaders(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders("Access-Control-Request-Headers"));
        if (values.isEmpty()) { return true; }
        if (values.size() != 1) { return false; }
        String[] headers = values.getFirst().split(",", 17);
        if (headers.length > 16) { return false; }
        Set<String> seen = new HashSet<>();
        for (String item : headers) {
            String header = item.trim();
            String normalized = header.toLowerCase(java.util.Locale.ROOT);
            if (header.length() > 64 || !header.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                    || !HEADERS.contains(normalized) || !seen.add(normalized)) { return false; }
        }
        return true;
    }

    public static boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain");
        response.getWriter().write("Invalid CORS request");
        return false;
    }
}
