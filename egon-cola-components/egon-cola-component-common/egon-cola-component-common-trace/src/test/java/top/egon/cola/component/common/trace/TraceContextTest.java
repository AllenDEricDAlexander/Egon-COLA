package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void rootContainsCompleteTraceStateAndCurrentMdc() {
        MDC.put("tenantId", "tenant-1");

        TraceContext context = TraceContext.root("request-1")
                .withSource("order-service", "order-1");

        assertTrue(context.traceId().matches("[0-9a-f]{32}"));
        assertTrue(context.spanId().matches("[0-9a-f]{16}"));
        assertNull(context.parentSpanId());
        assertEquals("request-1", context.requestId());
        assertEquals("00", context.traceFlags());
        assertEquals("order-service", context.sourceApp());
        assertEquals("order-1", context.sourceInstance());
        assertEquals("tenant-1", context.mdcContext().get("tenantId"));
    }

    @Test
    void extractsW3cParentAndCreatesCurrentSpan() {
        Map<String, String> headers = Map.of(
                TraceContext.TRACEPARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-"
                        + "00f067aa0ba902b7-01",
                TraceContext.TRACESTATE_HEADER,
                "vendor=value",
                TraceContext.REQUEST_ID_HEADER,
                "request-1"
        );

        TraceContext context = TraceContext.fromHeaders(headers::get, true);

        assertEquals(
                "4bf92f3577b34da6a3ce929d0e0e4736",
                context.traceId()
        );
        assertEquals("00f067aa0ba902b7", context.parentSpanId());
        assertNotEquals(context.parentSpanId(), context.spanId());
        assertEquals("01", context.traceFlags());
        assertEquals("vendor=value", context.tracestate());
        assertEquals("request-1", context.requestId());
    }

    @Test
    void createsChildWithoutChangingTraceOrBusinessMdc() {
        MDC.put("tenantId", "tenant-1");
        TraceContext parent = TraceContext.root("request-1");

        TraceContext child = parent.child();

        assertEquals(parent.traceId(), child.traceId());
        assertEquals(parent.spanId(), child.parentSpanId());
        assertNotEquals(parent.spanId(), child.spanId());
        assertEquals("tenant-1", child.mdcContext().get("tenantId"));
    }

    @Test
    void capturedContextRestoresCompleteMdcAndPreviousWorkerMdc() {
        TraceContext request = TraceContext.root("request-1");
        TraceContext captured;
        try (TraceContext.Scope ignored = request.open()) {
            MDC.put("tenantId", "tenant-1");
            captured = TraceContext.capture();
        }
        MDC.put(TraceContext.TRACE_ID, "worker-trace");
        MDC.put("workerKey", "worker-value");

        try (TraceContext.Scope ignored = captured.open()) {
            assertEquals(request.traceId(), TraceContext.getTraceId());
            assertEquals("tenant-1", MDC.get("tenantId"));
            assertNull(MDC.get("workerKey"));
        }

        assertEquals("worker-trace", TraceContext.getTraceId());
        assertEquals("worker-value", MDC.get("workerKey"));
        assertNull(MDC.get("tenantId"));
    }

    @Test
    void preservesNonW3cTraceIdWhenCopyingMdc() {
        TraceContext.setTraceId("business-trace-id");
        TraceContext captured = TraceContext.capture();
        MDC.clear();

        try (TraceContext.Scope ignored = captured.open()) {
            assertEquals("business-trace-id", TraceContext.getTraceId());
            assertFalse(captured.hasTrace());
        }
    }

    @Test
    void validatesTraceparentWithoutCreatingParserObjects() {
        assertTrue(TraceContext.isValidTraceparent(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-"
                        + "00f067aa0ba902b7-01"
        ));
        assertFalse(TraceContext.isValidTraceparent("invalid"));
        assertFalse(TraceContext.isValidTraceparent(
                "00-00000000000000000000000000000000-"
                        + "00f067aa0ba902b7-01"
        ));
    }
}
