package top.egon.cola.component.common.trace;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonLogUtilTest {

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
    void rendersCompleteMdcBeforeBusinessFields() {
        TraceState trace = new TraceState(
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                "fedcba9876543210",
                "request-1",
                "01",
                "vendor=value",
                "order-service",
                "instance-1"
        );
        MDC.put("tenantId", "tenant-1");
        MDC.put("alpha", "first");

        try (TraceScope ignored = TraceContext.open(trace)) {
            CommonLogUtil.bizInfo(logger)
                    .biz("order")
                    .scene("create")
                    .success("order created");
        }

        ILoggingEvent event = onlyEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "traceId=0123456789abcdef0123456789abcdef "
                        + "spanId=0123456789abcdef "
                        + "parentSpanId=fedcba9876543210 "
                        + "requestId=request-1 traceFlags=01 "
                        + "tracestate=\"vendor=value\" sourceApp=order-service "
                        + "sourceInstance=instance-1 alpha=first "
                        + "tenantId=tenant-1 biz=order scene=create "
                        + "result=SUCCESS msg=\"order created\"",
                event.getFormattedMessage()
        );
        assertEquals(trace.traceId(), event.getMDCPropertyMap().get("traceId"));
        assertEquals("tenant-1", event.getMDCPropertyMap().get("tenantId"));
    }

    @Test
    void preservesThrowableAndMasksSensitiveExtensionFields() {
        IllegalStateException cause = new IllegalStateException("failed");

        CommonLogUtil.bizError(logger)
                .biz(" ")
                .scene(null)
                .errorCode("ORDER_FAILED")
                .field("accessToken", "secret-value")
                .fail("create\nfailed", cause);

        ILoggingEvent event = onlyEvent();
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals(
                "reason=\"create failed\" result=FAIL "
                        + "error_code=ORDER_FAILED error_msg=failed "
                        + "accessToken=***",
                event.getFormattedMessage()
        );
        assertNotNull(event.getThrowableProxy());
        assertEquals(cause.getClass().getName(),
                event.getThrowableProxy().getClassName());
        assertEquals(cause.getMessage(), event.getThrowableProxy().getMessage());
    }

    @Test
    void boundsCollectionsAndNormalizesValuesToOneLine() {
        List<Integer> billIds = IntStream.rangeClosed(1, 25)
                .boxed()
                .toList();

        CommonLogUtil.bizWarn(logger)
                .billIds(billIds)
                .field("note", "first\r\nsecond")
                .log("bounded");

        String message = onlyEvent().getFormattedMessage();
        assertTrue(message.contains("msg=bounded"));
        assertTrue(message.contains("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20...total=25"));
        assertTrue(message.contains("note=\"first  second\""));
        assertFalse(message.contains("\r"));
        assertFalse(message.contains("\n"));
    }

    @Test
    void truncatesEachRenderedValueToItsConfiguredBoundary() {
        CommonLogUtil.bizInfo(logger)
                .field("detail", "x".repeat(2_000))
                .log("bounded");

        String message = onlyEvent().getFormattedMessage();
        String detail = message.substring(message.indexOf("detail=") + 7);
        assertEquals(1_000, detail.length());
        assertTrue(detail.endsWith("...truncated"));
    }

    @Test
    void skipsValueFormattingWhenLevelIsDisabled() {
        logger.setLevel(Level.INFO);
        AtomicInteger toStringCalls = new AtomicInteger();
        Object value = new Object() {
            @Override
            public String toString() {
                toStringCalls.incrementAndGet();
                return "expensive";
            }
        };

        CommonLogUtil.bizDebug(logger)
                .field("value", value)
                .log("ignored");

        assertTrue(appender.list.isEmpty());
        assertEquals(0, toStringCalls.get());
    }

    private ILoggingEvent onlyEvent() {
        assertEquals(1, appender.list.size());
        return appender.list.getFirst();
    }
}
