package top.egon.cola.component.dtp.admin.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.autoconfigure.TraceAutoConfiguration;
import top.egon.cola.component.common.trace.autoconfigure.TraceServletFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ClassName: AdminTraceAutoConfigurationTest
 * @description: Admin 服务统一链路过滤器集成测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-08Month-07Day
 * @Version: 1.0
 */
class AdminTraceAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        TraceContext.clearOwnedKeys();
    }

    @Test
    void shouldUseCommonTraceServletFilterForW3cHeaders() {
        contextRunner.run(context -> {
            TraceServletFilter filter = context.getBean(TraceServletFilter.class);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/dtp/apps");
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.addHeader(
                    TraceKeys.TRACEPARENT_HEADER,
                    "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
            );
            request.addHeader(TraceKeys.TRACESTATE_HEADER, "vendor=value");
            request.addHeader(TraceKeys.REQUEST_ID_HEADER, "request-http-001");

            filter.doFilter(request, response, (req, res) -> {
                assertThat(TraceContext.current()).hasValueSatisfying(state -> {
                    assertThat(state.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
                    assertThat(state.parentSpanId()).isEqualTo("00f067aa0ba902b7");
                    assertThat(state.tracestate()).isEqualTo("vendor=value");
                    assertThat(state.requestId()).isEqualTo("request-http-001");
                });
            });

            assertThat(response.getHeader(TraceKeys.TRACEPARENT_HEADER))
                    .startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-");
            assertThat(response.getHeader(TraceKeys.REQUEST_ID_HEADER)).isEqualTo("request-http-001");
            assertThat(response.getHeader(TraceKeys.LEGACY_TRACE_ID_HEADER)).isNull();
            assertThat(TraceContext.current()).isEmpty();
        });
    }
}
