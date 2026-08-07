package top.egon.cola.component.gateway.contract.trace;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 网关一次调用使用的分布式链路上下文。
 *
 * <p>该类型负责校验并规范化入口请求的 W3C Trace Context，同时保存网关引擎 span、请求 ID
 * 以及头部冲突信息，供 HTTP 和 RPC provider 继续传播。
 */
public record GatewayTraceContext(
        String traceId,
        String requestId,
        String parentSpanId,
        String engineSpanId,
        String traceFlags,
        String tracestate,
        Source source,
        boolean headerConflict
) {

    private static final int TRACEPARENT_LENGTH = 55;

    private static final int MAX_HEADER_LENGTH = 512;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HexFormat HEX = HexFormat.of();

    public GatewayTraceContext {
        traceId = normalizeHex(traceId, 32, true);
        if (traceId == null) {
            throw new IllegalArgumentException("invalid traceId");
        }
        requestId = boundedValue(requestId, traceId);
        if (parentSpanId != null
                && normalizeHex(parentSpanId, 16, true) == null) {
            throw new IllegalArgumentException("invalid parentSpanId");
        }
        if (engineSpanId == null
                || normalizeHex(engineSpanId, 16, true) == null) {
            throw new IllegalArgumentException("invalid engineSpanId");
        }
        traceFlags = traceFlags == null
                || normalizeHex(traceFlags, 2, false) == null
                ? "00"
                : traceFlags.toLowerCase(Locale.ROOT);
        tracestate = boundedValue(tracestate, null);
        Objects.requireNonNull(source, "source");
    }

    public static GatewayTraceContext fromHeaders(
            String traceparent,
            String tracestate,
            String requestId) {
        String normalizedRequestId = boundedValue(
                requestId,
                newTraceId(),
                128
        );
        if (!isValidTraceparent(traceparent)) {
            return new GatewayTraceContext(
                    newTraceId(),
                    normalizedRequestId,
                    null,
                    newSpanId(),
                    "00",
                    null,
                    Source.GENERATED,
                    false
            );
        }
        String version = traceparent.substring(0, 2);
        String traceFlags = traceparent.substring(53, 55);
        return new GatewayTraceContext(
                traceparent.substring(3, 35),
                normalizedRequestId,
                traceparent.substring(36, 52),
                newSpanId(),
                "00".equals(version)
                        ? traceFlags
                        : sampled(traceFlags) ? "01" : "00",
                boundedValue(tracestate, null),
                Source.TRACEPARENT,
                false
        );
    }

    public String engineTraceparent() {
        return "00-"
                + traceId
                + "-"
                + engineSpanId
                + "-"
                + traceFlags;
    }

    public String childTraceparent(String childSpanId) {
        if (childSpanId == null
                || normalizeHex(childSpanId, 16, true) == null) {
            throw new IllegalArgumentException("invalid childSpanId");
        }
        return "00-" + traceId + "-" + childSpanId + "-" + traceFlags;
    }

    public String newChildSpanId() {
        return newSpanId();
    }

    public boolean sampled() {
        return sampled(traceFlags);
    }

    private static String boundedValue(String value, String fallback) {
        return boundedValue(value, fallback, MAX_HEADER_LENGTH);
    }

    private static String boundedValue(String value,
                                       String fallback,
                                       int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength
                || trimmed.indexOf('\r') >= 0
                || trimmed.indexOf('\n') >= 0) {
            return fallback;
        }
        return trimmed;
    }

    private static boolean isValidTraceparent(String value) {
        if (value == null
                || value.length() < TRACEPARENT_LENGTH
                || value.length() > MAX_HEADER_LENGTH
                || !value.equals(value.trim())
                || value.charAt(2) != '-'
                || value.charAt(35) != '-'
                || value.charAt(52) != '-') {
            return false;
        }
        String version = value.substring(0, 2);
        if (!isLowercaseHex(version)
                || "ff".equals(version)
                || normalizeHex(value.substring(3, 35), 32, true) == null
                || normalizeHex(value.substring(36, 52), 16, true) == null
                || !isLowercaseHex(value.substring(53, 55))) {
            return false;
        }
        return "00".equals(version)
                ? value.length() == TRACEPARENT_LENGTH
                : value.length() == TRACEPARENT_LENGTH
                || value.charAt(TRACEPARENT_LENGTH) == '-';
    }

    private static boolean sampled(String traceFlags) {
        return (Integer.parseInt(traceFlags, 16) & 1) == 1;
    }

    private static String normalizeHex(String value,
                                       int length,
                                       boolean rejectAllZeros) {
        if (value == null || value.length() != length) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!isLowercaseHex(normalized)
                || rejectAllZeros
                && normalized.chars().allMatch(character -> character == '0')) {
            return null;
        }
        return normalized;
    }

    private static boolean isLowercaseHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!((character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static String newTraceId() {
        return randomHex(16);
    }

    private static String newSpanId() {
        return randomHex(8);
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        String hex;
        do {
            RANDOM.nextBytes(value);
            hex = HEX.formatHex(value);
        } while (hex.chars().allMatch(character -> character == '0'));
        return hex;
    }

    /**
     * Trace ID 的来源，表示来自 W3C Header，还是由网关生成。
     */
    public enum Source {
        TRACEPARENT,
        GENERATED
    }
}
