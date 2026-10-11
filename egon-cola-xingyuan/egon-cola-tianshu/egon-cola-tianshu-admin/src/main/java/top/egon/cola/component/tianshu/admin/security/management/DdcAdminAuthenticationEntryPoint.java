package top.egon.cola.component.tianshu.admin.security.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public final class DdcAdminAuthenticationEntryPoint
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Qualifier("jacksonObjectMapper") private final ObjectMapper objectMapper;

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
        boolean delivered = status == HttpServletResponse.SC_UNAUTHORIZED
                ? IdpSecurityObservationFilter.beforeAuthenticationFailure(request) : IdpSecurityObservationFilter.beforeAuthorizationDenied(request);
        response.setStatus(delivered ? status : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Cache-Control", "no-store");
        if (delivered && status == HttpServletResponse.SC_UNAUTHORIZED) { response.setHeader("WWW-Authenticate", "Bearer"); }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "code", delivered ? code : "OAUTH_AUDIT_DELIVERY_UNAVAILABLE",
                "message", delivered ? code : "OAUTH_AUDIT_DELIVERY_UNAVAILABLE"
        ));
    }

}
