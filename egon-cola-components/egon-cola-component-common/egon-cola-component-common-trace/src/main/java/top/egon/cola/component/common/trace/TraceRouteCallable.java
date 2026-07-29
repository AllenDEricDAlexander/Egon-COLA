package top.egon.cola.component.common.trace;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Callable template that restores the trace snapshot captured at construction.
 *
 * @param <T> task result type
 */
public abstract class TraceRouteCallable<T> implements Callable<T> {

    private final TraceSnapshot snapshot;

    protected TraceRouteCallable() {
        this(TraceSnapshot.capture());
    }

    protected TraceRouteCallable(TraceSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public final T call() throws Exception {
        try (TraceScope ignored = snapshot.open()) {
            return doCall();
        }
    }

    /**
     * Executes the task under the captured trace context.
     *
     * @return task result
     * @throws Exception original task exception
     */
    protected abstract T doCall() throws Exception;
}
