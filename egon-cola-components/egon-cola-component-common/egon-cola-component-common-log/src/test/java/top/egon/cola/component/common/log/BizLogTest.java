package top.egon.cola.component.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BizLogTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(getClass().getName());
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        logger.detachAndStopAllAppenders();
        appender = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        logger.detachAndStopAllAppenders();
        appender.stop();
    }

    @Test
    void writesControlledBusinessFieldsAndTraceMdc() {
        TraceState trace = TraceState.root("request-1");

        try (TraceScope ignored = TraceContext.open(trace)) {
            BizLog.info(logger)
                    .biz("order")
                    .scene("create")
                    .step("persist")
                    .phase("END")
                    .billType("ORDER")
                    .billId(42L)
                    .bizId("order-42")
                    .status("SUCCESS")
                    .decision("ALLOW")
                    .errorCode("0")
                    .costMs(18L)
                    .log("order created");
        }

        ILoggingEvent event = onlyEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals("order created", event.getFormattedMessage());
        assertEquals(Map.ofEntries(
                Map.entry("biz", "order"),
                Map.entry("scene", "create"),
                Map.entry("step", "persist"),
                Map.entry("phase", "END"),
                Map.entry("bill_type", "ORDER"),
                Map.entry("bill_id", 42L),
                Map.entry("biz_id", "order-42"),
                Map.entry("status", "SUCCESS"),
                Map.entry("decision", "ALLOW"),
                Map.entry("error_code", "0"),
                Map.entry("cost_ms", 18L),
                Map.entry("msg", "order created")
        ), keyValues(event));
        assertEquals(trace.traceId(), event.getMDCPropertyMap().get("traceId"));
        assertEquals(trace.spanId(), event.getMDCPropertyMap().get("spanId"));
        assertEquals(trace.requestId(), event.getMDCPropertyMap().get("requestId"));
    }

    @Test
    void errorKeepsCauseAndSkipsBlankValues() {
        IllegalStateException cause = new IllegalStateException("failed");

        BizLog.error(logger)
                .biz(" ")
                .scene(null)
                .errorCode("ORDER_FAILED")
                .log("create failed", cause);

        ILoggingEvent event = onlyEvent();
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals(cause.getClass().getName(), event.getThrowableProxy().getClassName());
        assertEquals(cause.getMessage(), event.getThrowableProxy().getMessage());
        assertEquals(Map.of(
                "error_code", "ORDER_FAILED",
                "msg", "create failed"
        ), keyValues(event));
    }

    @Test
    void stringValuesAreSingleLineAndBounded() {
        String value = "first\r\nsecond" + "x".repeat(2_000);

        BizLog.warn(logger).status(value).log("bounded");

        Object status = keyValues(onlyEvent()).get("status");
        assertTrue(status instanceof String);
        String text = (String) status;
        assertFalse(text.contains("\r"));
        assertFalse(text.contains("\n"));
        assertTrue(text.length() <= 1_024);
    }

    private ILoggingEvent onlyEvent() {
        assertEquals(1, appender.list.size());
        return appender.list.getFirst();
    }

    private static Map<String, Object> keyValues(ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(
                        pair -> pair.key,
                        pair -> pair.value
                ));
    }
}
