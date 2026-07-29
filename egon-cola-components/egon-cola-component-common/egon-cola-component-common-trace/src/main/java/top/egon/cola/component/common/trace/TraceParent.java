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
        if (value == null || TraceIds.hasLineBreak(value)) {
            return Optional.empty();
        }
        String candidate = value.trim();
        if (candidate.length() != 55) {
            return Optional.empty();
        }
        String[] parts = candidate.split("-", -1);
        if (parts.length != 4) {
            return Optional.empty();
        }
        Optional<String> version = normalizeVersion(parts[0]);
        Optional<String> traceId = TraceIds.normalizeTraceId(parts[1]);
        Optional<String> spanId = TraceIds.normalizeSpanId(parts[2]);
        Optional<String> flags = TraceIds.normalizeTraceFlags(parts[3]);
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
                traceFlags,
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
}
