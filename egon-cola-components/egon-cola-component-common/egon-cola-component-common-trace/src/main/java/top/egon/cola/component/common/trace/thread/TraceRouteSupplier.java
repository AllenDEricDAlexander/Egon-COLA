package top.egon.cola.component.common.trace.thread;

import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplier template that restores the trace snapshot captured at construction.
 *
 * @param <T> supplied value type
 */
public abstract class TraceRouteSupplier<T> implements Supplier<T> {

    private final TraceSnapshot snapshot;

    protected TraceRouteSupplier() {
        this(TraceSnapshot.capture());
    }

    protected TraceRouteSupplier(TraceSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public final T get() {
        try (TraceScope ignored = snapshot.open()) {
            return doGet();
        }
    }

    /**
     * Executes the supplier under the captured trace context.
     *
     * @return supplied value
     */
    protected abstract T doGet();
}
