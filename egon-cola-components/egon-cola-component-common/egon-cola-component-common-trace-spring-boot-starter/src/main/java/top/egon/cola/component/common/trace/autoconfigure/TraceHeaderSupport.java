package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceIds;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.TraceParent;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceState;

final class TraceHeaderSupport {

    private TraceHeaderSupport() {
    }

    static TracePropagation.Extracted extract(HttpHeaders headers,
                                              TraceProperties properties) {
        if (!properties.getPropagation().isEnabled()) {
            return new TracePropagation.Extracted(
                    TraceState.root(),
                    TracePropagation.Source.GENERATED,
                    false
            );
        }
        TracePropagation.Extracted extracted = TracePropagation.extract(
                headers::getFirst,
                new TracePropagation.Options(
                        properties.getPropagation().isLegacyTraceIdReadOnly()
                )
        );
        if (!properties.getPropagation().isSourceHeaders()) {
            return extracted;
        }
        return new TracePropagation.Extracted(
                extracted.state().withSource(
                        headers.getFirst(TraceKeys.SOURCE_APP_HEADER),
                        headers.getFirst(TraceKeys.SOURCE_INSTANCE_HEADER)
                ),
                extracted.source(),
                extracted.headerConflict()
        );
    }

    static void inject(HttpHeaders headers,
                       TraceState state,
                       boolean takeOverExistingTraceparent) {
        if (takeOverExistingTraceparent
                || !TraceParent.parse(headers.getFirst(
                TraceKeys.TRACEPARENT_HEADER
        )).isPresent()) {
            headers.set(TraceKeys.TRACEPARENT_HEADER, state.traceparent());
        }
        if (state.tracestate() != null && !state.tracestate().isBlank()) {
            headers.set(TraceKeys.TRACESTATE_HEADER, state.tracestate());
        }
        if (headers.getFirst(TraceKeys.REQUEST_ID_HEADER) == null
                && state.requestId() != null
                && !state.requestId().isBlank()) {
            headers.set(TraceKeys.REQUEST_ID_HEADER, state.requestId());
        }
    }

    static TraceState outboundState() {
        TraceState current = top.egon.cola.component.common.trace.TraceContext
                .currentOrCreate();
        if (TraceIds.isValidSpanId(current.spanId())) {
            return current.child();
        }
        return TraceState.root(current.requestId());
    }
}
