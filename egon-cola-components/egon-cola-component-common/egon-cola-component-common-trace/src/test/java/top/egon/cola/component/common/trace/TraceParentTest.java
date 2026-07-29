package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceParentTest {

    @Test
    void parsesAndEncodesValidTraceparent() {
        TraceParent parent = TraceParent.parse(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        ).orElseThrow();

        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", parent.traceId());
        assertEquals("00f067aa0ba902b7", parent.spanId());
        assertEquals("01", parent.traceFlags());
        assertEquals(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                parent.value()
        );
    }

    @Test
    void rejectsInvalidTraceparentValues() {
        assertFalse(TraceParent.parse(null).isPresent());
        assertFalse(TraceParent.parse("ff-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01").isPresent());
        assertFalse(TraceParent.parse("00-00000000000000000000000000000000-00f067aa0ba902b7-01").isPresent());
        assertFalse(TraceParent.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01").isPresent());
        assertFalse(TraceParent.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-0g").isPresent());
        assertFalse(TraceParent.parse("00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01").isPresent());
        assertFalse(TraceParent.parse(" 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01").isPresent());
        assertFalse(TraceParent.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\r\nx: y").isPresent());
    }

    @Test
    void parsesKnownFieldsFromAValidFutureVersion() {
        TraceParent parent = TraceParent.parse(
                "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-extra"
        ).orElseThrow();

        assertEquals("01", parent.version());
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", parent.traceId());
        assertEquals("00f067aa0ba902b7", parent.spanId());
    }

    @Test
    void acceptsOnlyBoundedTracestateWithoutLineBreaks() {
        assertTrue(TraceParent.normalizeTracestate("vendor=value").isPresent());
        assertFalse(TraceParent.normalizeTracestate("vendor=value\nother=value").isPresent());
        assertFalse(TraceParent.normalizeTracestate("a".repeat(513)).isPresent());
    }
}
