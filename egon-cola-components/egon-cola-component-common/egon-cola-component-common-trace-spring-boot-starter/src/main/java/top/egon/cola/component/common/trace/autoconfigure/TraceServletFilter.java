package top.egon.cola.component.common.trace.autoconfigure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.IOException;

public class TraceServletFilter extends OncePerRequestFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TraceServletFilter.class);

    private final TraceProperties properties;

    public TraceServletFilter(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getServlet().getExcludedPaths().stream()
                .anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Throwable failure = null;
        TraceContext context = TraceHeaderSupport.extract(
                new ServletServerHttpRequest(request).getHeaders(),
                properties
        );
        try (TraceContext.Scope ignored = context.open()) {
            if (properties.getPropagation().isResponseHeaders()
                    && properties.getServlet().isResponseHeaders()) {
                response.setHeader(
                        TraceContext.TRACEPARENT_HEADER,
                        context.traceparent()
                );
                if (context.requestId() != null) {
                    response.setHeader(
                            TraceContext.REQUEST_ID_HEADER,
                            context.requestId()
                    );
                }
            }
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            if (properties.getServlet().isAccessLog()) {
                logAccess(request, response, context, startedAt, failure);
            }
        }
    }

    private void logAccess(HttpServletRequest request,
                           HttpServletResponse response,
                           TraceContext context,
                           long startedAt,
                           Throwable failure) {
        long costMs = (System.nanoTime() - startedAt) / 1_000_000L;
        String path = properties.getServlet().isRecordQuery()
                && request.getQueryString() != null
                ? request.getRequestURI() + "?" + request.getQueryString()
                : request.getRequestURI();
        String clientIp = properties.getServlet().isTrustedProxyHeaders()
                ? firstForwardedFor(request)
                : request.getRemoteAddr();
        String errorCode = failure == null ? null : failure.getClass().getSimpleName();
        String responseBytes = response.getHeader(HttpHeaders.CONTENT_LENGTH);
        LOGGER.info(
                "trace_access protocol={} method={} path={} status={} cost_ms={} "
                        + "traceId={} spanId={} requestId={} clientIp={} "
                        + "errorCode={} responseBytes={}",
                request.getProtocol(),
                request.getMethod(),
                path,
                response.getStatus(),
                costMs,
                context.traceId(),
                context.spanId(),
                context.requestId(),
                clientIp,
                errorCode,
                responseBytes
        );
    }

    private String firstForwardedFor(HttpServletRequest request) {
        String value = request.getHeader("X-Forwarded-For");
        if (value == null || value.isBlank()) {
            return request.getRemoteAddr();
        }
        int comma = value.indexOf(',');
        return comma < 0 ? value.trim() : value.substring(0, comma).trim();
    }
}
