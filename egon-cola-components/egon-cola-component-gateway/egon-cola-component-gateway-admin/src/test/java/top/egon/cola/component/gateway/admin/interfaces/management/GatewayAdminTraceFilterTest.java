package top.egon.cola.component.gateway.admin.interfaces.management;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayAdminTraceFilterTest {

    @Test
    void prefersTraceparentAndReturnsTheLogicalTrace() throws Exception {
        GatewayAdminTraceFilter filter = new GatewayAdminTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/gateway/admin/dashboard"
        );
        request.setRequestURI("/api/v1/gateway/admin/dashboard");
        request.addHeader(
                "traceparent",
                "00-0123456789abcdef0123456789abcdef-"
                        + "0123456789abcdef-01"
        );
        request.addHeader(
                "X-Trace-Id",
                "fedcba9876543210fedcba9876543210"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<GatewayTraceContext> observed =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (incoming, outgoing) -> observed.set(
                        (GatewayTraceContext) incoming.getAttribute(
                                GatewayAdminTraceFilter.TRACE_ATTRIBUTE
                        )
                )
        );

        assertEquals(
                "0123456789abcdef0123456789abcdef",
                response.getHeader("X-Trace-Id")
        );
        assertEquals(
                "0123456789abcdef0123456789abcdef",
                observed.get().traceId()
        );
    }
}
