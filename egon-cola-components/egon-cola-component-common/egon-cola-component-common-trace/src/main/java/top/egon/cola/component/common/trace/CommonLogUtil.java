package top.egon.cola.component.common.trace;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Common business logging utility that renders Trace and MDC fields directly
 * into a stable single-line message.
 */
public final class CommonLogUtil {

    private static final String EMPTY_VALUE = "-";

    private static final int MAX_VALUE_LENGTH = 1_000;

    private static final int MAX_COLLECTION_ITEMS = 20;

    private static final String TRUNCATED_SUFFIX = "...truncated";

    private static final List<String> MDC_FIELD_ORDER = List.of(
            TraceKeys.TRACE_ID,
            TraceKeys.SPAN_ID,
            TraceKeys.PARENT_SPAN_ID,
            TraceKeys.REQUEST_ID,
            TraceKeys.TRACE_FLAGS,
            TraceKeys.TRACESTATE,
            TraceKeys.SOURCE_APP,
            TraceKeys.SOURCE_INSTANCE
    );

    private static final List<String> BIZ_LOG_FIELD_ORDER = List.of(
            "biz",
            "scene",
            "step",
            "phase",
            "bill_type",
            "bill_id",
            "bill_no",
            "biz_id",
            "biz_uk",
            "status",
            "expected_status",
            "from_status",
            "to_status",
            "decision",
            "reason",
            "result",
            "error_code",
            "error_msg",
            "changed",
            "cost_ms",
            "msg"
    );

    private CommonLogUtil() {
    }

    public static BizLogBuilder bizDebug(Logger logger) {
        return builder(logger, LogLevel.DEBUG);
    }

    public static BizLogBuilder bizInfo(Logger logger) {
        return builder(logger, LogLevel.INFO);
    }

    public static BizLogBuilder bizWarn(Logger logger) {
        return builder(logger, LogLevel.WARN);
    }

    public static BizLogBuilder bizError(Logger logger) {
        return builder(logger, LogLevel.ERROR);
    }

    private static BizLogBuilder builder(Logger logger, LogLevel level) {
        return new BizLogBuilder(
                Objects.requireNonNull(logger, "logger"),
                level
        );
    }

    /**
     * Stable business-log phases for common processing steps.
     */
    public enum Phase {
        START,
        LOAD,
        CHECK,
        DECISION,
        CREATE,
        UPDATE,
        DELETE,
        CALL,
        SEND,
        PROCESS,
        END
    }

    /**
     * Fluent builder for bounded single-line business logs.
     */
    public static final class BizLogBuilder {

        private final Logger logger;

        private final LogLevel defaultLevel;

        private final LinkedHashMap<String, Object> fields =
                new LinkedHashMap<>();

        private BizLogBuilder(Logger logger, LogLevel defaultLevel) {
            this.logger = logger;
            this.defaultLevel = defaultLevel;
        }

        public BizLogBuilder biz(String value) {
            return field("biz", value);
        }

        public BizLogBuilder scene(String value) {
            return field("scene", value);
        }

        public BizLogBuilder step(String value) {
            return field("step", value);
        }

        public BizLogBuilder phase(Phase value) {
            return value == null ? this : phase(value.name());
        }

        public BizLogBuilder phase(String value) {
            return field("phase", value);
        }

        public BizLogBuilder bill(String billType,
                                 Object billId,
                                 Object billNo) {
            field("bill_type", billType);
            field("bill_id", billId);
            field("bill_no", billNo);
            return this;
        }

        public BizLogBuilder billIds(Collection<?> billIds) {
            return field("bill_ids", billIds);
        }

        public BizLogBuilder bizId(Object value) {
            return field("biz_id", value);
        }

        public BizLogBuilder bizUk(Object value) {
            return field("biz_uk", value);
        }

        public BizLogBuilder status(Object value) {
            return field("status", value);
        }

        public BizLogBuilder expectedStatus(Object value) {
            return field("expected_status", value);
        }

        public BizLogBuilder statusChange(Object fromStatus, Object toStatus) {
            field("from_status", fromStatus);
            field("to_status", toStatus);
            return this;
        }

        public BizLogBuilder decision(boolean decision, String reason) {
            field("decision", decision ? "YES" : "NO");
            field("reason", reason);
            return this;
        }

        public BizLogBuilder decision(String decision, String reason) {
            field("decision", decision);
            field("reason", reason);
            return this;
        }

        public BizLogBuilder reason(String value) {
            return field("reason", value);
        }

        public BizLogBuilder changed(String value) {
            return field("changed", value);
        }

        public BizLogBuilder errorCode(String value) {
            return field("error_code", value);
        }

        public BizLogBuilder costMs(Object value) {
            return field("cost_ms", value);
        }

        public BizLogBuilder costSince(long startTimeMillis) {
            return costMs(System.currentTimeMillis() - startTimeMillis);
        }

        public BizLogBuilder msg(String value) {
            return field("msg", value);
        }

        public BizLogBuilder field(String key, Object value) {
            String normalizedKey = normalizeKey(key);
            if (normalizedKey == null || value == null) {
                return this;
            }
            if (value instanceof String text && text.isBlank()) {
                return this;
            }
            fields.put(normalizedKey, value);
            return this;
        }

        public BizLogBuilder fields(Map<String, ?> values) {
            if (values != null) {
                values.forEach(this::field);
            }
            return this;
        }

        public void start(String message) {
            field("result", "START");
            msg(message);
            log(LogLevel.INFO, null);
        }

        public void success(String message) {
            field("result", "SUCCESS");
            msg(message);
            log(defaultLevel, null);
        }

        public void log(String message) {
            msg(message);
            log(defaultLevel, null);
        }

        public void reject(String reason) {
            field("result", "REJECT");
            reason(reason);
            log(LogLevel.WARN, null);
        }

        public void warn(String message) {
            field("result", "WARN");
            msg(message);
            log(LogLevel.WARN, null);
        }

        public void fail(String reason) {
            field("result", "FAIL");
            reason(reason);
            log(LogLevel.ERROR, null);
        }

        public void fail(String reason, Throwable cause) {
            field("result", "FAIL");
            reason(reason);
            if (cause != null && cause.getMessage() != null
                    && !cause.getMessage().isBlank()) {
                field("error_msg", cause.getMessage());
            }
            log(LogLevel.ERROR, cause);
        }

        private void log(LogLevel level, Throwable cause) {
            if (!isEnabled(logger, level)) {
                return;
            }
            String message = buildLogMessage(
                    MDC.getCopyOfContextMap(),
                    fields
            );
            switch (level) {
                case DEBUG -> logger.debug(message);
                case INFO -> logger.info(message);
                case WARN -> {
                    if (cause == null) {
                        logger.warn(message);
                    } else {
                        logger.warn(message, cause);
                    }
                }
                case ERROR -> {
                    if (cause == null) {
                        logger.error(message);
                    } else {
                        logger.error(message, cause);
                    }
                }
            }
        }
    }

    private enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static boolean isEnabled(Logger logger, LogLevel level) {
        return switch (level) {
            case DEBUG -> logger.isDebugEnabled();
            case INFO -> logger.isInfoEnabled();
            case WARN -> logger.isWarnEnabled();
            case ERROR -> logger.isErrorEnabled();
        };
    }

    private static String buildLogMessage(Map<String, String> mdc,
                                          LinkedHashMap<String, Object> fields) {
        StringBuilder builder = new StringBuilder();
        appendMdcFields(builder, mdc, fields.keySet());
        appendBusinessFields(builder, fields);
        return builder.toString();
    }

    private static void appendMdcFields(StringBuilder builder,
                                        Map<String, String> mdc,
                                        Set<String> overriddenKeys) {
        if (mdc == null || mdc.isEmpty()) {
            return;
        }
        Set<String> writtenKeys = new HashSet<>();
        for (String key : MDC_FIELD_ORDER) {
            appendMdcField(
                    builder,
                    key,
                    mdc.get(key),
                    overriddenKeys,
                    writtenKeys
            );
        }
        mdc.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> appendMdcField(
                        builder,
                        entry.getKey(),
                        entry.getValue(),
                        overriddenKeys,
                        writtenKeys
                ));
    }

    private static void appendMdcField(StringBuilder builder,
                                       String key,
                                       String value,
                                       Set<String> overriddenKeys,
                                       Set<String> writtenKeys) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null || writtenKeys.contains(normalizedKey)
                || overriddenKeys.contains(normalizedKey)) {
            return;
        }
        appendField(builder, normalizedKey, value);
        writtenKeys.add(normalizedKey);
    }

    private static void appendBusinessFields(
            StringBuilder builder,
            LinkedHashMap<String, Object> fields) {
        if (fields.isEmpty()) {
            return;
        }
        Set<String> writtenKeys = new HashSet<>();
        for (String key : BIZ_LOG_FIELD_ORDER) {
            if (fields.containsKey(key)) {
                appendField(builder, key, fields.get(key));
                writtenKeys.add(key);
            }
        }
        fields.forEach((key, value) -> {
            if (!writtenKeys.contains(key)) {
                appendField(builder, key, value);
            }
        });
    }

    private static void appendField(StringBuilder builder,
                                    String key,
                                    Object value) {
        if (key == null || value == null) {
            return;
        }
        String formattedValue = formatValue(key, value);
        if (formattedValue == null) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(key).append('=').append(formattedValue);
    }

    private static String formatValue(String key, Object value) {
        String text = objectToString(value);
        text = normalizeValue(text);
        text = maskIfNecessary(key, text);
        text = truncate(text, MAX_VALUE_LENGTH);
        if (text.isBlank()) {
            text = EMPTY_VALUE;
        }
        return needQuote(text)
                ? '"' + escapeQuote(text) + '"'
                : text;
    }

    private static String objectToString(Object value) {
        if (value instanceof Collection<?> collection) {
            return collectionToString(collection);
        }
        if (value instanceof Map<?, ?> map) {
            return mapToString(map);
        }
        if (value.getClass().isArray()) {
            return arrayToString(value);
        }
        return String.valueOf(value);
    }

    private static String collectionToString(Collection<?> collection) {
        if (collection.isEmpty()) {
            return EMPTY_VALUE;
        }
        StringBuilder builder = new StringBuilder();
        Iterator<?> iterator = collection.iterator();
        int count = 0;
        while (iterator.hasNext() && count < MAX_COLLECTION_ITEMS) {
            appendCollectionSeparator(builder, count);
            builder.append(String.valueOf(iterator.next()));
            count++;
        }
        appendTotal(builder, collection.size());
        return builder.toString();
    }

    private static String arrayToString(Object array) {
        int length = Array.getLength(array);
        if (length == 0) {
            return EMPTY_VALUE;
        }
        StringBuilder builder = new StringBuilder();
        int count = Math.min(length, MAX_COLLECTION_ITEMS);
        for (int index = 0; index < count; index++) {
            appendCollectionSeparator(builder, index);
            builder.append(String.valueOf(Array.get(array, index)));
        }
        appendTotal(builder, length);
        return builder.toString();
    }

    private static String mapToString(Map<?, ?> map) {
        if (map.isEmpty()) {
            return EMPTY_VALUE;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= MAX_COLLECTION_ITEMS) {
                break;
            }
            appendCollectionSeparator(builder, count);
            builder.append(String.valueOf(entry.getKey()))
                    .append(':')
                    .append(String.valueOf(entry.getValue()));
            count++;
        }
        appendTotal(builder, map.size());
        return builder.toString();
    }

    private static void appendCollectionSeparator(StringBuilder builder,
                                                  int index) {
        if (index > 0) {
            builder.append(',');
        }
    }

    private static void appendTotal(StringBuilder builder, int size) {
        if (size > MAX_COLLECTION_ITEMS) {
            builder.append("...total=").append(size);
        }
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return EMPTY_VALUE;
        }
        return value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }

    private static boolean needQuote(String text) {
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (Character.isWhitespace(current) || current == '='
                    || current == '"' || current == '\'') {
                return true;
            }
        }
        return false;
    }

    private static String escapeQuote(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int contentLength = maxLength - TRUNCATED_SUFFIX.length();
        return text.substring(0, contentLength) + TRUNCATED_SUFFIX;
    }

    private static String maskIfNecessary(String key, String value) {
        String lowerKey = key.toLowerCase(java.util.Locale.ROOT);
        if (lowerKey.contains("password") || lowerKey.contains("passwd")
                || lowerKey.contains("pwd") || lowerKey.contains("token")
                || lowerKey.contains("secret")
                || lowerKey.contains("authorization")
                || lowerKey.contains("cookie")) {
            return "***";
        }
        if (lowerKey.contains("phone") || lowerKey.contains("mobile")) {
            return maskMiddle(value, 3, 4);
        }
        if (lowerKey.contains("bank_account")
                || lowerKey.contains("bank_card")
                || lowerKey.contains("card_no")
                || lowerKey.contains("id_card")
                || lowerKey.contains("cert_no")
                || lowerKey.contains("identity_no")) {
            return maskMiddle(value, 4, 4);
        }
        if (lowerKey.contains("email")) {
            return maskEmail(value);
        }
        return value;
    }

    private static String maskMiddle(String value,
                                     int prefixLength,
                                     int suffixLength) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= prefixLength + suffixLength) {
            return "***";
        }
        return value.substring(0, prefixLength)
                + "****"
                + value.substring(value.length() - suffixLength);
    }

    private static String maskEmail(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int separator = value.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return value.charAt(0) + "****" + value.substring(separator);
    }
}
