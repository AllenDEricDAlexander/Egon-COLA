package top.egon.cola.archetype.source.agent.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResult;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Map;

/** Isolated legacy fixture; production authentication is owned by the native USER boundary. */
@Deprecated
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ResearchApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Research-Api-Key";

    @Qualifier("legacyResearchApiProperties")
    private final DeepResearchApiProperties properties;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("agentClock")
    private final Clock clock;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presented = request.getHeader(API_KEY_HEADER);
        byte[] expected = properties.apiKey() == null
                ? new byte[0] : properties.apiKey().getBytes(StandardCharsets.UTF_8);
        byte[] actual = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
        if (presented == null || !MessageDigest.isEqual(expected, actual)) {
            writeUnauthorized(response, traceId(request));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String traceId) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setHeader("WWW-Authenticate", "ApiKey");
        response.setHeader(ResearchTraceFilter.TRACE_HEADER, traceId);
        objectMapper.writeValue(response.getWriter(), new DeepResearchErrorResult(
                ResearchErrorCodeEnum.RESEARCH_UNAUTHORIZED.code(),
                ResearchErrorCodeEnum.RESEARCH_UNAUTHORIZED.safeMessage(), traceId,
                clock.instant(), Map.of()));
    }

    private static String traceId(HttpServletRequest request) {
        Object trace = request.getAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE);
        return trace == null ? "unknown" : trace.toString();
    }
}
