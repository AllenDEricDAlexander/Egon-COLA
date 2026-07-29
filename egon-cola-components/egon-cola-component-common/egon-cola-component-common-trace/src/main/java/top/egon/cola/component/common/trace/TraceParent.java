package top.egon.cola.component.common.trace;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * Parsed W3C {@code traceparent} value.
 */
public record TraceParent(
        String version,
        String traceId,
        String spanId,
        String traceFlags
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int BASE_VALUE_LENGTH = 55;

    private static final int MAX_VALUE_LENGTH = 512;

    public TraceParent {
        version = normalizeVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("invalid traceparent version"));
        traceId = TraceIds.normalizeTraceId(traceId)
                .orElseThrow(() -> new IllegalArgumentException("invalid traceId"));
        spanId = TraceIds.normalizeSpanId(spanId)
                .orElseThrow(() -> new IllegalArgumentException("invalid spanId"));
        traceFlags = TraceIds.normalizeTraceFlags(traceFlags)
                .orElseThrow(() -> new IllegalArgumentException("invalid traceFlags"));
    }

    public static Optional<TraceParent> parse(String value) {
        if (value == null
                || TraceIds.hasLineBreak(value)
                || value.length() < BASE_VALUE_LENGTH
                || value.length() > MAX_VALUE_LENGTH
                || !value.equals(value.trim())) {
            return Optional.empty();
        }
        String candidate = value;
        if (candidate.charAt(2) != '-'
                || candidate.charAt(35) != '-'
                || candidate.charAt(52) != '-') {
            return Optional.empty();
        }
        String versionValue = candidate.substring(0, 2);
        String traceIdValue = candidate.substring(3, 35);
        String spanIdValue = candidate.substring(36, 52);
        String flagsValue = candidate.substring(53, 55);
        if (!isLowercaseHex(versionValue)
                || !isLowercaseHex(traceIdValue)
                || !isLowercaseHex(spanIdValue)
                || !isLowercaseHex(flagsValue)) {
            return Optional.empty();
        }
        Optional<String> version = normalizeVersion(versionValue);
        if (version.isEmpty()
                || ("00".equals(version.get())
                && candidate.length() != BASE_VALUE_LENGTH)
                || (!"00".equals(version.get())
                && candidate.length() > BASE_VALUE_LENGTH
                && candidate.charAt(BASE_VALUE_LENGTH) != '-')) {
            return Optional.empty();
        }
        Optional<String> traceId = TraceIds.normalizeTraceId(traceIdValue);
        Optional<String> spanId = TraceIds.normalizeSpanId(spanIdValue);
        Optional<String> flags = TraceIds.normalizeTraceFlags(flagsValue);
        if (version.isEmpty()
                || traceId.isEmpty()
                || spanId.isEmpty()
                || flags.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TraceParent(
                version.get(),
                traceId.get(),
                spanId.get(),
                flags.get()
        ));
    }

    public static Optional<String> normalizeTracestate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (TraceIds.hasLineBreak(value)) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.length() <= 512
                ? Optional.of(trimmed)
                : Optional.empty();
    }

    public String value() {
        return version + "-" + traceId + "-" + spanId + "-" + traceFlags;
    }

    public TraceState toChildState(String tracestate, String requestId) {
        return new TraceState(
                traceId,
                TraceIds.newSpanId(),
                spanId,
                requestId,
                outgoingTraceFlags(),
                tracestate,
                null,
                null
        );
    }

    private static Optional<String> normalizeVersion(String value) {
        Optional<String> normalized = TraceIds.normalizeTraceFlags(value);
        if (normalized.isEmpty() || "ff".equals(normalized.get())) {
            return Optional.empty();
        }
        return Optional.of(normalized.get());
    }

    private String outgoingTraceFlags() {
        if ("00".equals(version)) {
            return traceFlags;
        }
        return (Integer.parseInt(traceFlags, 16) & 1) == 1
                ? "01"
                : "00";
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
}
