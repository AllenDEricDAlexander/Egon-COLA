package top.egon.cola.component.common.log;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight builder restricted to the shared business log schema.
 */
public final class BizLogBuilder {

    private static final int MAX_VALUE_LENGTH = 1_024;
    private static final int MAX_MESSAGE_LENGTH = 4_096;

    private final Logger logger;
    private final Level level;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    BizLogBuilder(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    public BizLogBuilder biz(String value) {
        return put(BizLogFields.BIZ, value);
    }

    public BizLogBuilder scene(String value) {
        return put(BizLogFields.SCENE, value);
    }

    public BizLogBuilder step(String value) {
        return put(BizLogFields.STEP, value);
    }

    public BizLogBuilder phase(String value) {
        return put(BizLogFields.PHASE, value);
    }

    public BizLogBuilder billType(String value) {
        return put(BizLogFields.BILL_TYPE, value);
    }

    public BizLogBuilder billId(Object value) {
        return put(BizLogFields.BILL_ID, value);
    }

    public BizLogBuilder bizId(Object value) {
        return put(BizLogFields.BIZ_ID, value);
    }

    public BizLogBuilder status(Object value) {
        return put(BizLogFields.STATUS, value);
    }

    public BizLogBuilder decision(Object value) {
        return put(BizLogFields.DECISION, value);
    }

    public BizLogBuilder errorCode(String value) {
        return put(BizLogFields.ERROR_CODE, value);
    }

    public BizLogBuilder costMs(long value) {
        if (value >= 0) {
            fields.put(BizLogFields.COST_MS, value);
        }
        return this;
    }

    public void log(String message) {
        log(message, null);
    }

    public void log(String message, Throwable cause) {
        if (!logger.isEnabledForLevel(level)) {
            return;
        }
        String normalizedMessage = normalize(message, MAX_MESSAGE_LENGTH);
        LoggingEventBuilder event = logger.atLevel(level);
        fields.forEach(event::addKeyValue);
        if (!normalizedMessage.isBlank()) {
            event.addKeyValue(
                    BizLogFields.MSG,
                    normalize(normalizedMessage, MAX_VALUE_LENGTH)
            );
        }
        if (cause != null) {
            event.setCause(cause);
        }
        event.log(normalizedMessage);
    }

    private BizLogBuilder put(String key, Object value) {
        Object normalized = normalize(value);
        if (normalized != null) {
            fields.put(key, normalized);
        }
        return this;
    }

    private static Object normalize(Object value) {
        if (value instanceof String text) {
            String normalized = normalize(text, MAX_VALUE_LENGTH);
            return normalized.isBlank() ? null : normalized;
        }
        return value;
    }

    private static String normalize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
