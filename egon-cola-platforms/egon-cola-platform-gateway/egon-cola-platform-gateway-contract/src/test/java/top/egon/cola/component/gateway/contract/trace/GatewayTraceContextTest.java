package top.egon.cola.component.gateway.contract.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTraceContextTest {

    @Test
    void usesTraceparentWithoutLegacyHeaders() {
        GatewayTraceContext context = GatewayTraceContext.fromHeaders(
                "00-0123456789abcdef0123456789abcdef-"
                        + "0123456789abcdef-01",
                "vendor=value",
                null
        );

        assertEquals(
                "0123456789abcdef0123456789abcdef",
                context.traceId()
        );
        assertEquals(
                GatewayTraceContext.Source.TRACEPARENT,
                context.source()
        );
        assertFalse(context.headerConflict());
        assertTrue(context.sampled());
        assertNotEquals(context.parentSpanId(), context.engineSpanId());
    }

    @Test
    void carriesRequestIdAndGeneratesForInvalidValues() {
        GatewayTraceContext request = GatewayTraceContext.fromHeaders(
                "00-0123456789abcdef0123456789abcdef-"
                        + "0123456789abcdef-00",
                null,
                "request-1"
        );
        assertEquals("request-1", request.requestId());

        GatewayTraceContext header = GatewayTraceContext.fromHeaders(
                "invalid",
                null,
                null
        );
        assertEquals(
                GatewayTraceContext.Source.GENERATED,
                header.source()
        );
        assertTrue(header.traceId().matches("[0-9a-f]{32}"));

        GatewayTraceContext generated = GatewayTraceContext.fromHeaders(
                "00-00000000000000000000000000000000-"
                        + "0000000000000000-01",
                null,
                null
        );
        assertEquals(
                GatewayTraceContext.Source.GENERATED,
                generated.source()
        );
        assertTrue(generated.traceId().matches("[0-9a-f]{32}"));
    }
}
