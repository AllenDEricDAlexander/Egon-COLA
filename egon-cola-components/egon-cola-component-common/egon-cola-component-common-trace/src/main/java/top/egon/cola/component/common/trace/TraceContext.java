package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

import java.util.Optional;

/**
 * Trace context helper backed by a thread-local state and an MDC projection.
 */
public final class TraceContext {

    public static final String TRACE_ID = TraceKeys.TRACE_ID;

    private static final ThreadLocal<TraceState> CURRENT = new ThreadLocal<>();

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID);
            CURRENT.remove();
            return;
        }
        String value = traceId.trim().toLowerCase(java.util.Locale.ROOT);
        MDC.put(TRACE_ID, value);
        if (TraceIds.isValidTraceId(value)) {
            String spanId = MDC.get(TraceKeys.SPAN_ID);
            CURRENT.set(new TraceState(
                    value,
                    TraceIds.isValidSpanId(spanId)
                            ? spanId
                            : TraceIds.newSpanId(),
                    null,
                    MDC.get(TraceKeys.REQUEST_ID),
                    MDC.get(TraceKeys.TRACE_FLAGS),
                    MDC.get(TraceKeys.TRACESTATE),
                    MDC.get(TraceKeys.SOURCE_APP),
                    MDC.get(TraceKeys.SOURCE_INSTANCE)
            ));
        }
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
        CURRENT.remove();
    }

    public static Optional<TraceState> current() {
        TraceState current = CURRENT.get();
        if (current != null) {
            return Optional.of(current);
        }
        return TraceState.fromMdc(MDC.getCopyOfContextMap());
    }

    public static TraceState currentOrCreate() {
        return current().orElseGet(TraceState::root);
    }

    public static TraceScope open(TraceState state) {
        return TraceScope.open(state);
    }

    public static void clearOwnedKeys() {
        TraceKeys.ownedMdcKeys().forEach(MDC::remove);
        CURRENT.remove();
    }

    public static void putIfAbsent(String key, String value) {
        if (key == null || key.isBlank()
                || value == null || value.isBlank()
                || MDC.get(key) != null) {
            return;
        }
        MDC.put(key, value);
    }

    public static TraceSnapshot snapshot() {
        return TraceSnapshot.capture();
    }

    static TraceState currentThreadState() {
        return CURRENT.get();
    }

    static void setCurrentThreadState(TraceState state) {
        if (state == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(state);
        }
    }
}
