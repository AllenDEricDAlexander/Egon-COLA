package top.egon.cola.component.common.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.enums.BusinessExceptionEnum;
import top.egon.cola.component.common.core.enums.ExceptionCategory;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassificationTest {
    @Test
    void separatesBusinessAlertsFromLevelAndRetryability() {
        BusinessException normal = new BusinessException(BusinessExceptionEnum.INVALID_PARAM,
                ExceptionLevelEnum.ERROR, "password=secret");
        BusinessException alert = BusinessException.alert(BusinessExceptionEnum.OPERATION_FAILED, null, "order");
        CommonException system = new CommonException(ResultCode.REMOTE_CALL_ERROR, true, null);
        assertEquals(ExceptionCategory.BUSINESS, normal.getExceptionCategory());
        assertFalse(normal.isAlertRequired());
        assertEquals(ExceptionLevelEnum.ERROR, normal.getLevel());
        assertEquals(ExceptionCategory.BUSINESS_ALERT, alert.getExceptionCategory());
        assertTrue(alert.isAlertRequired());
        assertFalse(alert.isRetryable());
        assertEquals(ExceptionCategory.SYSTEM, system.getExceptionCategory());
        assertTrue(system.isAlertRequired());
        assertTrue(system.isRetryable());
    }

    @Test
    void wrappingRetainsKnownAlertsButAnOrdinaryCauseDoesNotCreateOne() {
        CommonException cause = new CommonException(ResultCode.MIDDLEWARE_ERROR);
        BusinessException wrapped = BusinessException.get(cause, BusinessExceptionEnum.OPERATION_FAILED,
                ExceptionLevelEnum.DEBUG, "order");
        assertEquals(ExceptionCategory.BUSINESS_ALERT, wrapped.getExceptionCategory());
        assertEquals(ExceptionLevelEnum.ERROR, wrapped.getLevel());
        assertSame(cause, wrapped.getCause());
        assertFalse(new BusinessException(BusinessExceptionEnum.INVALID_PARAM,
                new IllegalArgumentException("invalid")).isAlertRequired());
        BusinessException normal = new BusinessException(BusinessExceptionEnum.INVALID_PARAM);
        assertSame(normal, BusinessException.get((Exception) normal));
        assertTrue(BusinessException.get(new IllegalStateException("unknown")).isAlertRequired());
        assertTrue(new BusinessException(BusinessExceptionEnum.SYSTEM_ERROR).isAlertRequired());
        assertTrue(new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), "masked").isAlertRequired());
    }

    @Test
    void recordsOneRedactedEventWithStructuredMetadataAndDiagnosticFrames() {
        Logger logger = (Logger) LoggerFactory.getLogger("classification-redaction-test");
        ListAppender<ILoggingEvent> events = attach(logger);
        RuntimeException cause = new RuntimeException("Bearer sk-secret requestBody=password");
        cause.addSuppressed(new IllegalStateException("document secret"));
        BusinessException failure = BusinessException.alert(BusinessExceptionEnum.OPERATION_FAILED, cause, "token=secret");
        assertTrue(events.list.isEmpty());
        MDC.put("traceId", "0123456789abcdef0123456789abcdef");
        MDC.put("requestId", "request-7");
        try {
            ExceptionLogger.log(logger, failure);
            assertEquals(1, events.list.size());
            ILoggingEvent event = events.list.getFirst();
            var fields = event.getKeyValuePairs().stream().collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
            assertEquals(Level.ERROR, event.getLevel());
            assertEquals("BUSINESS_ALERT", fields.get("exceptionCategory"));
            assertEquals(true, fields.get("alertRequired"));
            assertEquals(failure.getCode(), fields.get("code"));
            assertEquals("request-7", fields.get("requestId"));
            assertEquals("0123456789abcdef0123456789abcdef", fields.get("traceId"));
            assertFalse(fields.containsKey("details"));
            assertEquals("exception handled", event.getFormattedMessage());
            assertFalse(event.getThrowableProxy().getMessage().contains("secret"));
            assertFalse(event.getThrowableProxy().getCause().getMessage().contains("secret"));
            assertFalse(event.getThrowableProxy().getCause().getSuppressed()[0].getMessage().contains("secret"));
            assertEquals(failure.getStackTrace()[0], event.getThrowableProxy().getStackTraceElementProxyArray()[0].getStackTraceElement());
            assertSame(cause, failure.getCause());
            assertTrue(cause.getMessage().contains("secret"));
        } finally {
            MDC.clear();
            logger.detachAppender(events);
        }
    }

    @Test
    void ordinaryBusinessUsesExplicitLevelWithoutAlertOrFreeFormMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger("classification-business-test");
        ListAppender<ILoggingEvent> events = attach(logger);
        try {
            ExceptionLogger.log(logger, new BusinessException(BusinessExceptionEnum.INVALID_PARAM));
            ExceptionLogger.log(logger, new BusinessException(BusinessExceptionEnum.INVALID_PARAM, ExceptionLevelEnum.INFO, "secret"));
            assertEquals(Level.WARN, events.list.get(0).getLevel());
            assertEquals(Level.INFO, events.list.get(1).getLevel());
            assertNull(events.list.get(0).getThrowableProxy());
            assertTrue(events.list.get(0).getKeyValuePairs().stream().anyMatch(pair -> pair.key.equals("alertRequired") && Boolean.FALSE.equals(pair.value)));
        } finally {
            logger.detachAppender(events);
        }
    }

    @Test
    void unknownAndAsyncFailuresHaveCorrectFallbackWhileFatalKeepsItsMarker() {
        Logger logger = (Logger) LoggerFactory.getLogger("classification-fallback-test");
        ListAppender<ILoggingEvent> events = attach(logger);
        try {
            ExceptionLogger.log(logger, new RuntimeException("unknown secret"));
            ExceptionLogger.log(logger, new CompletionException(new BusinessException(BusinessExceptionEnum.INVALID_PARAM)));
            ExceptionLogger.log(logger, new BusinessException(BusinessExceptionEnum.SYSTEM_ERROR, ExceptionLevelEnum.FATAL, "secret"));
            assertEquals(Level.ERROR, events.list.get(0).getLevel());
            assertEquals(Level.WARN, events.list.get(1).getLevel());
            assertEquals(Level.ERROR, events.list.get(2).getLevel());
            assertTrue(events.list.get(2).getKeyValuePairs().stream().anyMatch(pair -> pair.key.equals("exceptionLevel") && pair.value.equals("FATAL")));
        } finally {
            logger.detachAppender(events);
        }
    }

    @Test
    void cyclicCausesTerminateClassificationAndRedaction() {
        BusinessException failure = new BusinessException(BusinessExceptionEnum.INVALID_PARAM);
        RuntimeException cause = new RuntimeException("secret");
        RuntimeException second = new RuntimeException("secret");
        cause.initCause(second);
        second.initCause(cause);
        failure = new BusinessException(BusinessExceptionEnum.INVALID_PARAM, cause);
        assertFalse(failure.isAlertRequired());
        Logger logger = (Logger) LoggerFactory.getLogger("classification-cycle-test");
        ListAppender<ILoggingEvent> events = attach(logger);
        try {
            ExceptionLogger.log(logger, BusinessException.alert(BusinessExceptionEnum.OPERATION_FAILED, cause));
            assertEquals(1, events.list.size());
        } finally {
            logger.detachAppender(events);
        }
    }

    @Test
    void serializationAndPublicResultShapeRemainCompatible() throws Exception {
        BusinessException failure = BusinessException.alert(BusinessExceptionEnum.OPERATION_FAILED, null, "order");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(failure); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            BusinessException restored = (BusinessException) input.readObject();
            assertTrue(restored.isAlertRequired());
            assertEquals(failure.getCode(), restored.getCode());
        }
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(ResultRecord.failure(failure)));
        assertFalse(json.has("exceptionCategory"));
        assertFalse(json.has("alertRequired"));
        assertEquals(failure.getCode(), json.get("code").asInt());
        assertEquals(failure.getStatus(), json.get("status").asText());
        var category = CommonException.class.getDeclaredField("exceptionCategory");
        category.setAccessible(true);
        category.set(failure, null);
        assertEquals(ExceptionCategory.BUSINESS, failure.getExceptionCategory());
        CommonException legacySystem = new CommonException(ResultCode.SYSTEM_ERROR);
        category.set(legacySystem, null);
        assertTrue(legacySystem.isAlertRequired());
        BusinessException legacyBusinessAlert = new BusinessException(BusinessExceptionEnum.SYSTEM_ERROR);
        category.set(legacyBusinessAlert, null);
        assertTrue(legacyBusinessAlert.isAlertRequired());
        assertTrue(new BusinessException(BusinessExceptionEnum.OPERATION_FAILED, legacyBusinessAlert).isAlertRequired());
    }

    private static ListAppender<ILoggingEvent> attach(Logger logger) {
        logger.setLevel(Level.TRACE);
        logger.setAdditive(false);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        return events;
    }
}
