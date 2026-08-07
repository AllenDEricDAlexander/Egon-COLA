package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceContext;

final class TraceHeaderSupport {

    private TraceHeaderSupport() {
    }

    static TraceContext extract(HttpHeaders headers,
                                TraceProperties properties) {
        if (!properties.getPropagation().isEnabled()) {
            return TraceContext.root();
        }
        return TraceContext.fromHeaders(
                headers::getFirst,
                properties.getPropagation().isLegacyTraceIdReadOnly()
        );
    }

    static void inject(HttpHeaders headers,
                       TraceContext context,
                       boolean takeOverExistingTraceparent) {
        if (takeOverExistingTraceparent
                || !TraceContext.isValidTraceparent(headers.getFirst(
                TraceContext.TRACEPARENT_HEADER
        ))) {
            headers.set(
                    TraceContext.TRACEPARENT_HEADER,
                    context.traceparent()
            );
        }
        if (context.tracestate() != null) {
            headers.set(
                    TraceContext.TRACESTATE_HEADER,
                    context.tracestate()
            );
        }
        if (headers.getFirst(TraceContext.REQUEST_ID_HEADER) == null
                && context.requestId() != null) {
            headers.set(
                    TraceContext.REQUEST_ID_HEADER,
                    context.requestId()
            );
        }
    }

    static TraceContext outboundContext() {
        return TraceContext.currentOrCreate().child();
    }
}
