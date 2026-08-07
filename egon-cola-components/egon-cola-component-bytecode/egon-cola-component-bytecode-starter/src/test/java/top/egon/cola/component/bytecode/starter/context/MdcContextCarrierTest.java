package top.egon.cola.component.bytecode.starter.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcContextCarrierTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void restoresCapturedContextAndThenRestoresWorkerContext() {
        MdcContextCarrier carrier = new MdcContextCarrier();
        TraceContext expected = TraceContext.root("request-bytecode");
        Object snapshot;
        try (TraceContext.Scope ignored = expected.open()) {
            MDC.put("businessId", "order-001");
            snapshot = carrier.capture();
        }
        MDC.put("traceId", "worker");

        try (var ignored = carrier.restore(snapshot)) {
            TraceContext actual = TraceContext.capture();
            assertEquals(expected.traceId(), actual.traceId());
            assertEquals(expected.spanId(), actual.spanId());
            assertEquals(expected.requestId(), actual.requestId());
            assertEquals("order-001", MDC.get("businessId"));
            MDC.put("traceId", "business");
        }

        assertEquals("worker", MDC.get("traceId"));
    }

    @Test
    void supportsEmptyCapturedAndWorkerContextsWithoutLeaks() {
        MdcContextCarrier carrier = new MdcContextCarrier();
        Object snapshot = carrier.capture();
        MDC.put("traceId", "worker");

        try (var ignored = carrier.restore(snapshot)) {
            assertNull(MDC.get("traceId"));
        }

        assertEquals("worker", MDC.get("traceId"));
    }
}
