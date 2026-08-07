package top.egon.cola.component.dtp.registry.model;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.trace.TraceKeys;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @ClassName: DtpConfigChangeMessageTest
 * @description: 动态线程池配置变更消息链路载体测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-08Month-07Day
 * @Version: 1.0
 */
class DtpConfigChangeMessageTest {

    @Test
    void shouldKeepOnlyImmutableW3cTraceCarrierValues() {
        Map<String, String> carrier = new LinkedHashMap<>();
        carrier.put(
                TraceKeys.TRACEPARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );
        carrier.put(TraceKeys.TRACESTATE_HEADER, "vendor=value");
        carrier.put(TraceKeys.REQUEST_ID_HEADER, "request-001");
        carrier.put(TraceKeys.LEGACY_TRACE_ID_HEADER, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setTraceContext(carrier);
        carrier.put(TraceKeys.REQUEST_ID_HEADER, "mutated");

        assertEquals(3, message.getTraceContext().size());
        assertEquals("request-001", message.getTraceContext().get(TraceKeys.REQUEST_ID_HEADER));
        assertFalse(message.getTraceContext().containsKey(TraceKeys.LEGACY_TRACE_ID_HEADER));
        assertThrows(
                UnsupportedOperationException.class,
                () -> message.getTraceContext().put("unexpected", "value")
        );
    }

    @Test
    void shouldUseEmptyCarrierForNullInput() {
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();

        message.setTraceContext(null);

        assertEquals(Map.of(), message.getTraceContext());
    }
}
