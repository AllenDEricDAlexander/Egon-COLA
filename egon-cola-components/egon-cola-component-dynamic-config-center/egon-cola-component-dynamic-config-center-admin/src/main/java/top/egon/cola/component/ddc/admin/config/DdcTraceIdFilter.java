package top.egon.cola.component.ddc.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.IOException;
import java.util.UUID;

@Component
public class DdcTraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = StringUtils.hasText(request.getHeader(TRACE_ID_HEADER))
                ? request.getHeader(TRACE_ID_HEADER)
                : UUID.randomUUID().toString().replace("-", "");
        try {
            TraceContext.setTraceId(traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clearTraceId();
        }
    }
}
