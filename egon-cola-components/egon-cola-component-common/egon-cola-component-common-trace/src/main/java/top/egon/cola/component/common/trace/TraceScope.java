package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Restores trace state and MDC after a scoped operation.
 */
public final class TraceScope implements AutoCloseable {

    private final TraceState previousState;

    private final Map<String, String> previousOwnedMdc;

    private final Map<String, String> previousFullMdc;

    private final boolean fullMdcRestore;

    private final AtomicBoolean closed = new AtomicBoolean();

    private TraceScope(TraceState previousState,
                       Map<String, String> previousOwnedMdc,
                       Map<String, String> previousFullMdc,
                       boolean fullMdcRestore) {
        this.previousState = previousState;
        this.previousOwnedMdc = previousOwnedMdc;
        this.previousFullMdc = previousFullMdc;
        this.fullMdcRestore = fullMdcRestore;
    }

    static TraceScope open(TraceState state) {
        if (state == null) {
            throw new IllegalArgumentException("trace state must not be null");
        }
        TraceState previousState = TraceContext.currentThreadState();
        Map<String, String> previousOwned = ownedMdcSnapshot();
        TraceContext.setCurrentThreadState(state);
        applyTraceState(state);
        return new TraceScope(previousState, previousOwned, null, false);
    }

    static TraceScope open(TraceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, String> previousFullMdc = MDC.getCopyOfContextMap();
        TraceState previousState = TraceContext.currentThreadState();
        restoreFullMdc(snapshot.mdcContext());
        TraceContext.setCurrentThreadState(snapshot.state());
        return new TraceScope(previousState, null, previousFullMdc, true);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        TraceContext.setCurrentThreadState(previousState);
        if (fullMdcRestore) {
            restoreFullMdc(previousFullMdc);
        } else {
            restoreOwnedMdc(previousOwnedMdc);
        }
    }

    private static Map<String, String> ownedMdcSnapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String key : TraceKeys.ownedMdcKeys()) {
            String value = MDC.get(key);
            if (value != null) {
                snapshot.put(key, value);
            }
        }
        return snapshot;
    }

    private static void applyTraceState(TraceState state) {
        restoreOwnedMdc(state.toMdcMap());
    }

    private static void restoreOwnedMdc(Map<String, String> snapshot) {
        for (String key : TraceKeys.ownedMdcKeys()) {
            String value = snapshot == null ? null : snapshot.get(key);
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }
    }

    private static void restoreFullMdc(Map<String, String> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(snapshot);
        }
    }
}
