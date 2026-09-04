package top.egon.cola.archetype.source.agent.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Normalizes a safe trace id before authentication and keeps it in request scope only. */
@Component("researchTraceFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ResearchTraceFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_ATTRIBUTE = "research.traceId";

    @Qualifier("agentClock")
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(TRACE_HEADER);
        String traceId = supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.trim();
        if (!valid(traceId)) {
            writeError(response, UUID.randomUUID().toString());
            return;
        }
        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        MDC.put(TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_HEADER);
        }
    }

    private void writeError(HttpServletResponse response, String traceId) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setHeader(TRACE_HEADER, traceId);
        objectMapper.writeValue(response.getWriter(), new DeepResearchErrorResponse(
                ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR.code(),
                ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR.safeMessage(), traceId,
                clock.instant(), java.util.Map.of()));
    }

    private static boolean valid(String traceId) {
        return traceId != null && traceId.length() <= 128
                && traceId.matches("[A-Za-z0-9._:-]+");
    }
}
