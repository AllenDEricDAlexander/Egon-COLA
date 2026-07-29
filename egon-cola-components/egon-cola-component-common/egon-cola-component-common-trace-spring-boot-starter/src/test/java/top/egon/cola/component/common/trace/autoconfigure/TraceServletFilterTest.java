package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.common.trace.TraceKeys;

import static org.assertj.core.api.Assertions.assertThat;

class TraceServletFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void extractsTraceparentWritesW3cResponseHeadersAndRestoresMdc() throws Exception {
        TraceProperties properties = new TraceProperties();
        TraceServletFilter filter = new TraceServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(
                TraceKeys.TRACEPARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );
        request.addHeader("X-Trace-Id", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        MDC.put("biz", "keep");

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(TraceKeys.TRACE_ID))
                    .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
            assertThat(MDC.get(TraceKeys.PARENT_SPAN_ID))
                    .isEqualTo("00f067aa0ba902b7");
        });

        assertThat(response.getHeader(TraceKeys.TRACEPARENT_HEADER))
                .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-");
        assertThat(response.getHeader(TraceKeys.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(response.getHeader("x-egon-trace-id")).isNull();
        assertThat(MDC.get(TraceKeys.TRACE_ID)).isNull();
        assertThat(MDC.get("biz")).isEqualTo("keep");
    }

    @Test
    void excludesConfiguredPaths() throws Exception {
        TraceProperties properties = new TraceProperties();
        properties.getServlet().getExcludedPaths().add("/actuator/**");
        TraceServletFilter filter = new TraceServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(TraceKeys.TRACE_ID)).isNull()
        );

        assertThat(response.getHeader(TraceKeys.TRACEPARENT_HEADER)).isNull();
    }

    @Test
    void sourceHeadersRequireExplicitOptIn() throws Exception {
        TraceProperties properties = new TraceProperties();
        TraceServletFilter filter = new TraceServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceKeys.SOURCE_APP_HEADER, "client-app");

        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(TraceKeys.SOURCE_APP)).isNull()
        );

        properties.getPropagation().setSourceHeaders(true);
        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(TraceKeys.SOURCE_APP)).isEqualTo("client-app")
        );
    }
}
