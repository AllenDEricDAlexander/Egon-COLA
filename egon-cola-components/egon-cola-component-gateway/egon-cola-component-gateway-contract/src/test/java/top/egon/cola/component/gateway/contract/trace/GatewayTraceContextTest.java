package top.egon.cola.component.gateway.contract.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTraceContextTest {

    @Test
    void prefersTraceparentAndReportsConflict() {
        GatewayTraceContext context = GatewayTraceContext.select(
                "00-0123456789abcdef0123456789abcdef-"
                        + "0123456789abcdef-01",
                "fedcba9876543210fedcba9876543210",
                "vendor=value"
        );

        assertEquals(
                "0123456789abcdef0123456789abcdef",
                context.traceId()
        );
        assertEquals(
                GatewayTraceContext.Source.TRACEPARENT,
                context.source()
        );
        assertTrue(context.headerConflict());
        assertTrue(context.sampled());
        assertNotEquals(context.parentSpanId(), context.engineSpanId());
    }

    @Test
    void fallsBackToHeaderThenGeneratesForInvalidValues() {
        GatewayTraceContext header = GatewayTraceContext.select(
                "invalid",
                "ABCDEF0123456789ABCDEF0123456789",
                null
        );
        assertEquals(
                "abcdef0123456789abcdef0123456789",
                header.traceId()
        );
        assertEquals(
                GatewayTraceContext.Source.X_TRACE_ID,
                header.source()
        );

        GatewayTraceContext generated = GatewayTraceContext.select(
                "00-00000000000000000000000000000000-"
                        + "0000000000000000-01",
                "00000000000000000000000000000000",
                null
        );
        assertEquals(
                GatewayTraceContext.Source.GENERATED,
                generated.source()
        );
        assertTrue(generated.traceId().matches("[0-9a-f]{32}"));
    }
}
