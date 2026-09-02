package top.egon.cola.archetype.source.webopen.adapter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.io.IOException;

@Component("organizationTraceFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OrganizationTraceFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    private final LongIdGenerator idGenerator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String supplied = request.getHeader(TRACE_HEADER);
        String traceId = supplied == null || supplied.isBlank()
            ? Long.toString(idGenerator.nextLongId()) : supplied.trim();
        response.setHeader(TRACE_HEADER, traceId);
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
