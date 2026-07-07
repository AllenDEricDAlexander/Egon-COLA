package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void traceContextStoresTraceIdInMdc() {
        TraceContext.setTraceId("trace-1");

        assertEquals("trace-1", TraceContext.getTraceId());
    }

    @Test
    void blankTraceIdClearsContext() {
        TraceContext.setTraceId("trace-1");
        TraceContext.setTraceId(" ");

        assertNull(TraceContext.getTraceId());
    }

    @Test
    void snapshotCapturesCurrentTraceId() {
        TraceContext.setTraceId("trace-2");

        TraceSnapshot snapshot = TraceContext.snapshot();

        assertEquals("trace-2", snapshot.getTraceId());
    }
}
