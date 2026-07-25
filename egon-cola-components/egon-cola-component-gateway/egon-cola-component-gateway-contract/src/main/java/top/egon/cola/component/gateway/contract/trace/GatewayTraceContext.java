package top.egon.cola.component.gateway.contract.trace;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GatewayTraceContext(
        String traceId,
        String parentSpanId,
        String engineSpanId,
        String traceFlags,
        String tracestate,
        Source source,
        boolean headerConflict
) {

    private static final Pattern TRACE_ID =
            Pattern.compile("[0-9a-fA-F]{32}");

    private static final Pattern TRACEPARENT = Pattern.compile(
            "([0-9a-fA-F]{2})-([0-9a-fA-F]{32})-"
                    + "([0-9a-fA-F]{16})-([0-9a-fA-F]{2})"
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    public GatewayTraceContext {
        traceId = validTraceId(traceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("invalid traceId"));
        if (parentSpanId != null
                && (!parentSpanId.matches("[0-9a-f]{16}")
                || zeros(parentSpanId))) {
            throw new IllegalArgumentException("invalid parentSpanId");
        }
        if (engineSpanId == null
                || !engineSpanId.matches("[0-9a-f]{16}")
                || zeros(engineSpanId)) {
            throw new IllegalArgumentException("invalid engineSpanId");
        }
        traceFlags = traceFlags == null
                || !traceFlags.matches("[0-9a-f]{2}")
                ? "00"
                : traceFlags;
        tracestate = boundedTracestate(tracestate);
        Objects.requireNonNull(source, "source");
    }

    public static GatewayTraceContext select(
            String traceparent,
            String xTraceId,
            String tracestate) {
        ParsedParent parent = parseTraceparent(traceparent);
        String fallback = validTraceId(xTraceId).orElse(null);
        String traceId = parent == null
                ? fallback == null ? randomHex(16) : fallback
                : parent.traceId;
        return new GatewayTraceContext(
                traceId,
                parent == null ? null : parent.spanId,
                randomHex(8),
                parent == null ? "00" : parent.flags,
                tracestate,
                parent != null
                        ? Source.TRACEPARENT
                        : fallback != null
                        ? Source.X_TRACE_ID
                        : Source.GENERATED,
                parent != null
                        && fallback != null
                        && !parent.traceId.equals(fallback)
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
                || !childSpanId.matches("[0-9a-f]{16}")
                || zeros(childSpanId)) {
            throw new IllegalArgumentException("invalid childSpanId");
        }
        return "00-" + traceId + "-" + childSpanId + "-" + traceFlags;
    }

    public String newChildSpanId() {
        return randomHex(8);
    }

    public boolean sampled() {
        return (Integer.parseInt(traceFlags, 16) & 1) == 1;
    }

    private static ParsedParent parseTraceparent(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = TRACEPARENT.matcher(value.trim());
        if (!matcher.matches()
                || "ff".equalsIgnoreCase(matcher.group(1))) {
            return null;
        }
        String traceId = matcher.group(2).toLowerCase(Locale.ROOT);
        String spanId = matcher.group(3).toLowerCase(Locale.ROOT);
        if (zeros(traceId) || zeros(spanId)) {
            return null;
        }
        return new ParsedParent(
                traceId,
                spanId,
                matcher.group(4).toLowerCase(Locale.ROOT)
        );
    }

    private static java.util.Optional<String> validTraceId(String value) {
        if (value == null || !TRACE_ID.matcher(value.trim()).matches()) {
            return java.util.Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return zeros(normalized)
                ? java.util.Optional.empty()
                : java.util.Optional.of(normalized);
    }

    private static boolean zeros(String value) {
        return value.chars().allMatch(character -> character == '0');
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        do {
            RANDOM.nextBytes(value);
        } while (zeros(HexFormat.of().formatHex(value)));
        return HexFormat.of().formatHex(value);
    }

    private static String boundedTracestate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 512
                || trimmed.indexOf('\r') >= 0
                || trimmed.indexOf('\n') >= 0) {
            return null;
        }
        return trimmed;
    }

    public enum Source {
        TRACEPARENT,
        X_TRACE_ID,
        GENERATED
    }

    private record ParsedParent(
            String traceId,
            String spanId,
            String flags
    ) {
    }
}
