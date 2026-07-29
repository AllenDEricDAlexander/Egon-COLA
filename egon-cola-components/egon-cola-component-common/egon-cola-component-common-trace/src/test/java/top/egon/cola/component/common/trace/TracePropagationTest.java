package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracePropagationTest {

    @Test
    void validTraceparentWinsOverLegacyTraceHeaderAndMarksConflict() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        headers.put("X-Trace-Id", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::get,
                TracePropagation.Options.defaults()
        );

        assertEquals(TracePropagation.Source.TRACEPARENT, extracted.source());
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", extracted.state().traceId());
        assertEquals("00f067aa0ba902b7", extracted.state().parentSpanId());
        assertNotEquals("00f067aa0ba902b7", extracted.state().spanId());
        assertTrue(extracted.headerConflict());
    }

    @Test
    void legacyTraceHeaderIsReadOnlyAndNeverWritten() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Trace-Id", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::get,
                TracePropagation.Options.defaults()
        );

        Map<String, String> outbound = new LinkedHashMap<>();
        TracePropagation.inject(extracted.state(), outbound::put);

        assertEquals(TracePropagation.Source.X_TRACE_ID, extracted.source());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", extracted.state().traceId());
        assertTrue(outbound.containsKey("traceparent"));
        assertFalse(outbound.containsKey("x-egon-trace-id"));
        assertFalse(outbound.containsKey("X-Trace-Id"));
    }

    @Test
    void generatedTraceGetsRequestIdWhenInboundHeadersAreInvalid() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("traceparent", "00-00000000000000000000000000000000-00f067aa0ba902b7-01");
        headers.put("X-Trace-Id", "not-a-trace");

        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::get,
                TracePropagation.Options.defaults()
        );

        assertEquals(TracePropagation.Source.GENERATED, extracted.source());
        assertTrue(TraceIds.isValidTraceId(extracted.state().traceId()));
        assertTrue(TraceIds.isValidSpanId(extracted.state().spanId()));
        assertFalse(extracted.state().requestId().isBlank());
    }

    @Test
    void injectDoesNotWriteBlankTracestate() {
        TraceState state = TraceState.root().withTracestate(null);
        Map<String, String> headers = new LinkedHashMap<>();

        TracePropagation.inject(state, headers::put);

        assertNull(headers.get("tracestate"));
    }

    @Test
    void discardsTracestateWithoutAValidTraceparent() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("traceparent", "invalid");
        headers.put("tracestate", "vendor=value");

        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::get,
                TracePropagation.Options.defaults()
        );

        assertNull(extracted.state().tracestate());
    }

    @Test
    void replacesInvalidRequestIdWithABoundedGeneratedValue() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-egon-request-id", "x".repeat(129));

        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::get,
                TracePropagation.Options.defaults()
        );

        assertTrue(TraceIds.isValidTraceId(extracted.state().requestId()));
    }
}
