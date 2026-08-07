package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.common.trace.TraceContext;

import static org.assertj.core.api.Assertions.assertThat;

class TraceServletFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void extractsW3cContextWritesResponseAndRestoresMdc() throws Exception {
        TraceServletFilter filter = new TraceServletFilter(
                new TraceProperties()
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/orders/1"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(
                TraceContext.TRACEPARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-"
                        + "00f067aa0ba902b7-01"
        );
        request.addHeader(TraceContext.REQUEST_ID_HEADER, "request-1");
        MDC.put("biz", "keep");

        filter.doFilter(request, response, (req, res) -> {
            TraceContext current = TraceContext.current().orElseThrow();
            assertThat(current.traceId())
                    .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(current.parentSpanId())
                    .isEqualTo("00f067aa0ba902b7");
            assertThat(current.requestId()).isEqualTo("request-1");
            assertThat(MDC.get("biz")).isEqualTo("keep");
        });

        assertThat(response.getHeader(TraceContext.TRACEPARENT_HEADER))
                .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-");
        assertThat(response.getHeader(TraceContext.REQUEST_ID_HEADER))
                .isEqualTo("request-1");
        assertThat(TraceContext.current()).isEmpty();
        assertThat(MDC.get("biz")).isEqualTo("keep");
    }

    @Test
    void excludesConfiguredPaths() throws Exception {
        TraceProperties properties = new TraceProperties();
        properties.getServlet().getExcludedPaths().add("/actuator/**");
        TraceServletFilter filter = new TraceServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/actuator/health"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(TraceContext.current()).isEmpty()
        );

        assertThat(response.getHeader(
                TraceContext.TRACEPARENT_HEADER
        )).isNull();
    }
}
