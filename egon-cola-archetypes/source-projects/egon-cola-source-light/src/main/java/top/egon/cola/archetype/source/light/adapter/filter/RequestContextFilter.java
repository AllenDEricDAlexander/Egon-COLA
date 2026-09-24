package top.egon.cola.archetype.source.light.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.pojo.ResultRecord;

import java.io.IOException;
import java.util.UUID;

@Component("domainRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String OPERATOR_ID_HEADER = "X-Operator-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = attributeOrHeader(request);
        String operatorId = headerOrDefault(request, OPERATOR_ID_HEADER, "anonymous");
        String requestId = headerOrDefault(request, REQUEST_ID_HEADER, traceId);
        // These headers must be supplied or overwritten by the trusted ingress.
        MDC.remove("tenantId");
        MDC.put("userId", operatorId);
        MDC.put("requestId", requestId);
        try {
            String tenantHeader = headerOrDefault(request, TENANT_ID_HEADER, null);
            Long tenantId;
            try {
                tenantId = tenantHeader == null ? null : Long.valueOf(tenantHeader);
            } catch (NumberFormatException exception) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), ResultRecord.failure(
                        ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getStatus(),
                        "X-Tenant-Id must be a valid Long"));
                return;
            }
            if (tenantId != null) {
                MDC.put("tenantId", tenantId.toString());
            }
            RequestContextHolder.set(new RequestContext(operatorId, requestId, traceId, tenantId));
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
            MDC.remove("tenantId");
            MDC.remove("userId");
            MDC.remove("requestId");
        }
    }

    private static String attributeOrHeader(HttpServletRequest request) {
        Object attribute = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        if (attribute instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }
        return headerOrDefault(request, TraceIdFilter.TRACE_ID_HEADER, UUID.randomUUID().toString());
    }

    private static String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
