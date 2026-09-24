package top.egon.cola.archetype.source.lightopen.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestContextFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestContextFilter filter = new RequestContextFilter(objectMapper);

    @AfterEach
    void clear_context() {
        MDC.clear();
        RequestContextHolder.clear();
    }

    @Test
    void exposes_typed_context_and_persistence_mdc_and_always_clears_them() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestContextFilter.TENANT_ID_HEADER, " 0042 ");
        request.addHeader(RequestContextFilter.OPERATOR_ID_HEADER, "operator-1");
        request.addHeader(RequestContextFilter.REQUEST_ID_HEADER, "request-1");
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-1");
        MDC.put("traceId", "trace-1");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(RequestContextHolder.getRequired()).isEqualTo(
                    new RequestContext("operator-1", "request-1", "trace-1", 42L));
            assertThat(EgonColaTenantIdProvider.currentTenantId()).isEqualTo(42L);
            assertThat(MDC.get("tenantId")).isEqualTo("42");
            assertThat(MDC.get("userId")).isEqualTo("operator-1");
            assertThat(MDC.get("requestId")).isEqualTo("request-1");
        });

        assert_context_cleared();
        assertThat(MDC.get("traceId")).isEqualTo("trace-1");
    }

    @Test
    void clears_context_when_downstream_fails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestContextFilter.TENANT_ID_HEADER, "42");
        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            throw new ServletException("failed");
        })).isInstanceOf(ServletException.class);
        assert_context_cleared();
    }

    @Test
    void missing_tenant_never_reuses_previous_tenant_or_invents_a_default() throws Exception {
        MDC.put("tenantId", "99");
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
            assertThat(RequestContextHolder.getRequired().tenantId()).isNull();
            assertThat(MDC.get("tenantId")).isNull();
            assertThatThrownBy(EgonColaTenantIdProvider::currentTenantId)
                    .isInstanceOf(IllegalStateException.class).hasMessage("TENANT_CONTEXT_MISSING");
        });
        assert_context_cleared();
    }

    @Test
    void malformed_or_overflowing_tenant_returns_common_failure_before_business_code() throws Exception {
        for (String tenantId : new String[]{"invalid", "9223372036854775808"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(RequestContextFilter.TENANT_ID_HEADER, tenantId);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                throw new AssertionError("business code must not run");
            });
            assertThat(response.getStatus()).isEqualTo(400);
            var body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("success").booleanValue()).isFalse();
            assertThat(body.get("code").intValue()).isEqualTo(422000);
            assertThat(body.get("status").textValue()).isEqualTo("VALIDATION_ERROR");
            assert_context_cleared();
        }
    }

    private static void assert_context_cleared() {
        assertThat(RequestContextHolder.get()).isEmpty();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }
}
