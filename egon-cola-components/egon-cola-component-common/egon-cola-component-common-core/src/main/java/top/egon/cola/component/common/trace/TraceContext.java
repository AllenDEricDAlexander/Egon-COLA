package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

/**
 * TraceId context helper based on the MDC key traceId.
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID);
            return;
        }
        MDC.put(TRACE_ID, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    public static TraceSnapshot snapshot() {
        return new TraceSnapshot(getTraceId());
    }
}
