package top.egon.cola.component.bytecode.starter.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

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
        MDC.put("traceId", "submitter");
        Object snapshot = carrier.capture();
        MDC.put("traceId", "worker");

        try (var ignored = carrier.restore(snapshot)) {
            assertEquals("submitter", MDC.get("traceId"));
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
