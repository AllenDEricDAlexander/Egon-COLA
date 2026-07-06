package top.egon.cola.component.dtp.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * @author      有罗敷的马同学
 * @description 动态线程池请求链路标识过滤器
 * @Date        上午12:20 2026/6/30
 **/
@Component
public class DtpTraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";

    public static final String REQUEST_ID = "requestId";

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = StringUtils.hasText(request.getHeader(TRACE_ID_HEADER))
                ? request.getHeader(TRACE_ID_HEADER)
                : UUID.randomUUID().toString().replace("-", "");
        String requestId = StringUtils.hasText(request.getHeader(REQUEST_ID_HEADER))
                ? request.getHeader(REQUEST_ID_HEADER)
                : traceId;
        try {
            MDC.put(TRACE_ID, traceId);
            MDC.put(REQUEST_ID, requestId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }

}
