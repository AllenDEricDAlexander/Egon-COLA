package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

import java.util.Map;
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
        TraceState current = CURRENT.get();
        return current == null ? MDC.get(TRACE_ID) : current.traceId();
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            clearTraceId();
            return;
        }
        String value = traceId.trim();
        Optional<String> normalized = TraceIds.normalizeTraceId(value);
        if (normalized.isEmpty()) {
            clearOwnedKeys();
            MDC.put(TRACE_ID, value);
            return;
        }
        TraceState previous = current().orElse(null);
        TraceState state = previous != null
                && previous.traceId().equals(normalized.get())
                ? previous
                : new TraceState(
                normalized.get(),
                TraceIds.newSpanId(),
                null,
                previous == null
                        ? MDC.get(TraceKeys.REQUEST_ID)
                        : previous.requestId(),
                "00",
                null,
                previous == null
                        ? MDC.get(TraceKeys.SOURCE_APP)
                        : previous.sourceApp(),
                previous == null
                        ? MDC.get(TraceKeys.SOURCE_INSTANCE)
                        : previous.sourceInstance()
        );
        install(state);
    }

    public static void clearTraceId() {
        clearOwnedKeys();
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
        CURRENT.remove();
        replaceOwnedMdc(Map.of());
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

    private static void install(TraceState state) {
        CURRENT.set(state);
        replaceOwnedMdc(state.toMdcMap());
    }

    private static void replaceOwnedMdc(Map<String, String> context) {
        for (String key : TraceKeys.ownedMdcKeys()) {
            String value = context.get(key);
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }
    }
}
