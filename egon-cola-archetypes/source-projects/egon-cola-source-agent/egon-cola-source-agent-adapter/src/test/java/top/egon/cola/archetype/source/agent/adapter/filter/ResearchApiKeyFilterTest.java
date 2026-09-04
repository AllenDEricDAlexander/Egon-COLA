package top.egon.cola.archetype.source.agent.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ResearchApiKeyFilterTest {

    private final ResearchApiKeyFilter filter = new ResearchApiKeyFilter(
            new DeepResearchApiProperties("test-key"), new ObjectMapper().registerModule(new JavaTimeModule()),
            Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void rejects_missing_and_wrong_keys_with_the_same_safe_unauthorized_response() throws Exception {
        MockHttpServletRequest missing = request(null);
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        FilterChain missingChain = mock(FilterChain.class);
        filter.doFilter(missing, missingResponse, missingChain);

        MockHttpServletRequest wrong = request("wrong-key");
        MockHttpServletResponse wrongResponse = new MockHttpServletResponse();
        FilterChain wrongChain = mock(FilterChain.class);
        filter.doFilter(wrong, wrongResponse, wrongChain);

        assertEquals(401, missingResponse.getStatus());
        assertEquals(401, wrongResponse.getStatus());
        assertEquals(missingResponse.getContentAsString(), wrongResponse.getContentAsString());
        assertTrue(missingResponse.getHeader("WWW-Authenticate").contains("ApiKey"));
        verifyNoInteractions(missingChain, wrongChain);
    }

    @Test
    void accepts_exact_key_without_trimming_and_continues_the_chain() throws Exception {
        MockHttpServletRequest request = request("test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    private static MockHttpServletRequest request(String apiKey) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (apiKey != null) {
            request.addHeader(ResearchApiKeyFilter.API_KEY_HEADER, apiKey);
        }
        return request;
    }
}
