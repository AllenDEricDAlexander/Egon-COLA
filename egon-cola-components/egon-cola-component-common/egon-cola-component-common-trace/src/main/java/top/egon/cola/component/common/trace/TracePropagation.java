package top.egon.cola.component.common.trace;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Protocol-neutral trace extraction and injection.
 */
public final class TracePropagation {

    private TracePropagation() {
    }

    public static Extracted extract(TraceCarrierReader reader,
                                    Options options) {
        Objects.requireNonNull(reader, "reader");
        Options effectiveOptions = options == null
                ? Options.defaults()
                : options;
        String requestId = firstText(
                read(reader, TraceKeys.REQUEST_ID_HEADER),
                TraceIds.newTraceId()
        );
        String tracestate = TraceParent.normalizeTracestate(
                read(reader, TraceKeys.TRACESTATE_HEADER)
        ).orElse(null);
        TraceParent parent = TraceParent.parse(
                read(reader, TraceKeys.TRACEPARENT_HEADER)
        ).orElse(null);
        String legacyTraceId = effectiveOptions.readLegacyTraceId()
                ? TraceIds.normalizeTraceId(
                read(reader, TraceKeys.LEGACY_TRACE_ID_HEADER, "x-trace-id")
        ).orElse(null)
                : null;
        if (parent != null) {
            return new Extracted(
                    parent.toChildState(tracestate, requestId),
                    Source.TRACEPARENT,
                    legacyTraceId != null
                            && !legacyTraceId.equals(parent.traceId())
            );
        }
        if (legacyTraceId != null) {
            return new Extracted(
                    new TraceState(
                            legacyTraceId,
                            TraceIds.newSpanId(),
                            null,
                            requestId,
                            "00",
                            tracestate,
                            null,
                            null
                    ),
                    Source.X_TRACE_ID,
                    false
            );
        }
        return new Extracted(
                TraceState.root(requestId).withTracestate(tracestate),
                Source.GENERATED,
                false
        );
    }

    public static void inject(TraceState state, TraceCarrierWriter writer) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(writer, "writer");
        writer.set(TraceKeys.TRACEPARENT_HEADER, state.traceparent());
        if (state.tracestate() != null && !state.tracestate().isBlank()) {
            writer.set(TraceKeys.TRACESTATE_HEADER, state.tracestate());
        }
        if (state.requestId() != null && !state.requestId().isBlank()) {
            writer.set(TraceKeys.REQUEST_ID_HEADER, state.requestId());
        }
    }

    public static TraceState childForOutbound() {
        return TraceContext.currentOrCreate().child();
    }

    private static String read(TraceCarrierReader reader, String... names) {
        for (String name : names) {
            String value = reader.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(String value, String fallback) {
        return value == null || value.isBlank() || TraceIds.hasLineBreak(value)
                ? fallback
                : value.trim();
    }

    public enum Source {
        TRACEPARENT,
        X_TRACE_ID,
        GENERATED
    }

    public record Options(boolean readLegacyTraceId) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public static Options defaults() {
            return new Options(true);
        }
    }

    public record Extracted(
            TraceState state,
            Source source,
            boolean headerConflict
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
