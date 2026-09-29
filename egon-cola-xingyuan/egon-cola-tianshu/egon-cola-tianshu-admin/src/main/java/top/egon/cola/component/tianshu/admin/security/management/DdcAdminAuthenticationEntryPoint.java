package top.egon.cola.component.tianshu.admin.security.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public final class DdcAdminAuthenticationEntryPoint
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public DdcAdminAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "TIANSHU_ADMIN_AUTHENTICATION_REQUIRED"
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "TIANSHU_ADMIN_CAPABILITY_REQUIRED"
        );
    }

    /** Writes the stable security response when a browser's expected tenant no longer matches the authenticated USER tenant. */
    public void handleTenantMismatch(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.debug("Rejected Tianshu admin request because tenant context did not match");
        write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "TIANSHU_ADMIN_TENANT_MISMATCH"
        );
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code) throws IOException {
        response.setStatus(status);
        if (isAiPath(request)) response.setHeader("Cache-Control", "no-store");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", code,
                "message", code
        ));
    }

    private boolean isAiPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/v1/tianshu/ai/");
    }
}
