package top.egon.cola.component.common.trace;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable trace context state projected to MDC and propagation carriers.
 */
public record TraceState(
        String traceId,
        String spanId,
        String parentSpanId,
        String requestId,
        String traceFlags,
        String tracestate,
        String sourceApp,
        String sourceInstance
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public TraceState {
        traceId = TraceIds.normalizeTraceId(traceId)
                .orElseThrow(() -> new IllegalArgumentException("invalid traceId"));
        spanId = TraceIds.normalizeSpanId(spanId)
                .orElseThrow(() -> new IllegalArgumentException("invalid spanId"));
        parentSpanId = parentSpanId == null || parentSpanId.isBlank()
                ? null
                : TraceIds.normalizeSpanId(parentSpanId)
                .orElseThrow(() -> new IllegalArgumentException("invalid parentSpanId"));
        requestId = cleanValue(requestId, 128);
        traceFlags = traceFlags == null || traceFlags.isBlank()
                ? "00"
                : TraceIds.normalizeTraceFlags(traceFlags)
                .orElseThrow(() -> new IllegalArgumentException("invalid traceFlags"));
        tracestate = tracestate == null || tracestate.isBlank()
                ? null
                : TraceParent.normalizeTracestate(tracestate)
                .orElseThrow(() -> new IllegalArgumentException("invalid tracestate"));
        sourceApp = cleanValue(sourceApp, 128);
        sourceInstance = cleanValue(sourceInstance, 128);
    }

    public static TraceState root() {
        return root(null);
    }

    public static TraceState root(String requestId) {
        return new TraceState(
                TraceIds.newTraceId(),
                TraceIds.newSpanId(),
                null,
                requestId,
                "00",
                null,
                null,
                null
        );
    }

    public static Optional<TraceState> fromMdc(Map<String, String> mdc) {
        if (mdc == null || mdc.isEmpty()) {
            return Optional.empty();
        }
        String traceId = mdc.get(TraceKeys.TRACE_ID);
        String spanId = mdc.get(TraceKeys.SPAN_ID);
        if (!TraceIds.isValidTraceId(traceId)
                || !TraceIds.isValidSpanId(spanId)) {
            return Optional.empty();
        }
        return Optional.of(new TraceState(
                traceId,
                spanId,
                mdc.get(TraceKeys.PARENT_SPAN_ID),
                mdc.get(TraceKeys.REQUEST_ID),
                mdc.get(TraceKeys.TRACE_FLAGS),
                mdc.get(TraceKeys.TRACESTATE),
                mdc.get(TraceKeys.SOURCE_APP),
                mdc.get(TraceKeys.SOURCE_INSTANCE)
        ));
    }

    public TraceState child() {
        return new TraceState(
                traceId,
                TraceIds.newSpanId(),
                spanId,
                requestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withSpanId(String nextSpanId) {
        return new TraceState(
                traceId,
                nextSpanId,
                parentSpanId,
                requestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withParentSpanId(String nextParentSpanId) {
        return new TraceState(
                traceId,
                spanId,
                nextParentSpanId,
                requestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withRequestId(String nextRequestId) {
        return new TraceState(
                traceId,
                spanId,
                parentSpanId,
                nextRequestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withTracestate(String nextTracestate) {
        return new TraceState(
                traceId,
                spanId,
                parentSpanId,
                requestId,
                traceFlags,
                nextTracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withTraceFlags(String nextTraceFlags) {
        return new TraceState(
                traceId,
                spanId,
                parentSpanId,
                requestId,
                nextTraceFlags,
                tracestate,
                sourceApp,
                sourceInstance
        );
    }

    public TraceState withSource(String nextSourceApp, String nextSourceInstance) {
        return new TraceState(
                traceId,
                spanId,
                parentSpanId,
                requestId,
                traceFlags,
                tracestate,
                nextSourceApp,
                nextSourceInstance
        );
    }

    public Map<String, String> toMdcMap() {
        Map<String, String> mdc = new LinkedHashMap<>();
        put(mdc, TraceKeys.TRACE_ID, traceId);
        put(mdc, TraceKeys.SPAN_ID, spanId);
        put(mdc, TraceKeys.PARENT_SPAN_ID, parentSpanId);
        put(mdc, TraceKeys.REQUEST_ID, requestId);
        put(mdc, TraceKeys.TRACE_FLAGS, traceFlags);
        put(mdc, TraceKeys.TRACESTATE, tracestate);
        put(mdc, TraceKeys.SOURCE_APP, sourceApp);
        put(mdc, TraceKeys.SOURCE_INSTANCE, sourceInstance);
        return Map.copyOf(mdc);
    }

    public String traceparent() {
        return new TraceParent("00", traceId, spanId, traceFlags).value();
    }

    public boolean sampled() {
        return (Integer.parseInt(traceFlags, 16) & 1) == 1;
    }

    private static void put(Map<String, String> target,
                            String key,
                            String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String cleanValue(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength || TraceIds.hasLineBreak(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
