package top.egon.cola.component.gateway.contract.trace;

import top.egon.cola.component.common.trace.TraceCarrierReader;
import top.egon.cola.component.common.trace.TraceIds;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceState;

import java.util.Locale;
import java.util.Objects;

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

    public GatewayTraceContext {
        traceId = TraceIds.normalizeTraceId(traceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("invalid traceId"));
        requestId = boundedValue(requestId, traceId);
        if (parentSpanId != null
                && !TraceIds.isValidSpanId(parentSpanId)) {
            throw new IllegalArgumentException("invalid parentSpanId");
        }
        if (engineSpanId == null
                || !TraceIds.isValidSpanId(engineSpanId)) {
            throw new IllegalArgumentException("invalid engineSpanId");
        }
        traceFlags = traceFlags == null
                || !TraceIds.isValidTraceFlags(traceFlags)
                ? "00"
                : traceFlags.toLowerCase(Locale.ROOT);
        tracestate = boundedValue(tracestate, null);
        Objects.requireNonNull(source, "source");
    }

    public static GatewayTraceContext fromHeaders(
            String traceparent,
            String tracestate,
            String requestId) {
        TracePropagation.Extracted extracted = TracePropagation.extract(
                reader(traceparent, tracestate, requestId),
                new TracePropagation.Options(false)
        );
        TraceState state = extracted.state();
        return new GatewayTraceContext(
                state.traceId(),
                state.requestId(),
                state.parentSpanId(),
                state.spanId(),
                state.traceFlags(),
                state.tracestate(),
                source(extracted.source()),
                extracted.headerConflict()
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
                || !TraceIds.isValidSpanId(childSpanId)) {
            throw new IllegalArgumentException("invalid childSpanId");
        }
        return "00-" + traceId + "-" + childSpanId + "-" + traceFlags;
    }

    public String newChildSpanId() {
        return TraceIds.newSpanId();
    }

    public boolean sampled() {
        return (Integer.parseInt(traceFlags, 16) & 1) == 1;
    }

    private static String boundedValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 512
                || trimmed.indexOf('\r') >= 0
                || trimmed.indexOf('\n') >= 0) {
            return fallback;
        }
        return trimmed;
    }

    private static TraceCarrierReader reader(
            String traceparent,
            String tracestate,
            String requestId) {
        return name -> {
            if (TraceKeys.TRACEPARENT_HEADER.equals(name)) {
                return traceparent;
            }
            if (TraceKeys.TRACESTATE_HEADER.equals(name)) {
                return tracestate;
            }
            if (TraceKeys.REQUEST_ID_HEADER.equals(name)) {
                return requestId;
            }
            return null;
        };
    }

    private static Source source(TracePropagation.Source source) {
        return source == TracePropagation.Source.TRACEPARENT
                ? Source.TRACEPARENT
                : Source.GENERATED;
    }

    public enum Source {
        TRACEPARENT,
        GENERATED
    }
}
