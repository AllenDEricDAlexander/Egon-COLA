package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdsTest {

    @Test
    void generatesW3cTraceAndSpanIds() {
        String traceId = TraceIds.newTraceId();
        String spanId = TraceIds.newSpanId();

        assertTrue(traceId.matches("[0-9a-f]{32}"));
        assertTrue(spanId.matches("[0-9a-f]{16}"));
        assertTrue(TraceIds.isValidTraceId(traceId));
        assertTrue(TraceIds.isValidSpanId(spanId));
    }

    @Test
    void rejectsAllZeroAndUuidTraceIds() {
        assertFalse(TraceIds.isValidTraceId("00000000000000000000000000000000"));
        assertFalse(TraceIds.isValidSpanId("0000000000000000"));
        assertFalse(TraceIds.isValidTraceId("550e8400-e29b-41d4-a716-446655440000"));
    }
}
