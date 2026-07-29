package top.egon.cola.component.common.trace;

import java.util.Set;

/**
 * Shared trace MDC keys and protocol header names.
 */
public final class TraceKeys {

    public static final String TRACE_ID = "traceId";

    public static final String SPAN_ID = "spanId";

    public static final String PARENT_SPAN_ID = "parentSpanId";

    public static final String REQUEST_ID = "requestId";

    public static final String TRACE_FLAGS = "traceFlags";

    public static final String TRACESTATE = "tracestate";

    public static final String SOURCE_APP = "sourceApp";

    public static final String SOURCE_INSTANCE = "sourceInstance";

    public static final String TRACEPARENT_HEADER = "traceparent";

    public static final String TRACESTATE_HEADER = "tracestate";

    public static final String REQUEST_ID_HEADER = "x-egon-request-id";

    public static final String LEGACY_TRACE_ID_HEADER = "X-Trace-Id";

    public static final String SOURCE_APP_HEADER = "x-egon-source-app";

    public static final String SOURCE_INSTANCE_HEADER = "x-egon-source-instance";

    private static final Set<String> OWNED_MDC_KEYS = Set.of(
            TRACE_ID,
            SPAN_ID,
            PARENT_SPAN_ID,
            REQUEST_ID,
            TRACE_FLAGS,
            TRACESTATE,
            SOURCE_APP,
            SOURCE_INSTANCE
    );

    private TraceKeys() {
    }

    public static Set<String> ownedMdcKeys() {
        return OWNED_MDC_KEYS;
    }
}
