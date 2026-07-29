package top.egon.cola.component.common.trace.thread;

import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;

import java.util.Objects;

/**
 * Runnable template that restores the trace snapshot captured at construction.
 */
public abstract class TraceRouteRunnable implements Runnable {

    private final TraceSnapshot snapshot;

    protected TraceRouteRunnable() {
        this(TraceSnapshot.capture());
    }

    protected TraceRouteRunnable(TraceSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public final void run() {
        try (TraceScope ignored = snapshot.open()) {
            doRun();
        }
    }

    /**
     * Executes the task under the captured trace context.
     */
    protected abstract void doRun();
}
