package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.io.IOException;

public class GatewayAdminTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_ATTRIBUTE =
            GatewayAdminTraceFilter.class.getName() + ".trace";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/gateway/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        GatewayTraceContext trace = GatewayTraceContext.select(
                request.getHeader("traceparent"),
                request.getHeader("X-Trace-Id"),
                request.getHeader("tracestate")
        );
        request.setAttribute(TRACE_ATTRIBUTE, trace);
        response.setHeader("X-Trace-Id", trace.traceId());
        MDC.put("traceId", trace.traceId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
