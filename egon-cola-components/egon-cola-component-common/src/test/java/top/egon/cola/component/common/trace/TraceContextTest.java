package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getsAndSetsTraceIdByStandardKey() {
        TraceContext.setTraceId("trace-001");

        assertEquals("trace-001", TraceContext.getTraceId());
        assertEquals("trace-001", MDC.get("traceId"));
    }

    @Test
    void doesNotReadAliasKeys() {
        MDC.put("trace_id", "trace-alias");

        assertNull(TraceContext.getTraceId());
    }

    @Test
    void clearsTraceIdOnly() {
        MDC.put("traceId", "trace-001");
        MDC.put("other", "value");

        TraceContext.clearTraceId();

        assertNull(MDC.get("traceId"));
        assertEquals("value", MDC.get("other"));
    }
}
