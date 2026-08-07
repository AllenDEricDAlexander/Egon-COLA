package top.egon.cola.component.common.trace.thread;

import top.egon.cola.component.common.trace.TraceContext;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplier template that restores the trace context captured at construction.
 *
 * @param <T> supplied value type
 */
public abstract class TraceRouteSupplier<T> implements Supplier<T> {

    private final TraceContext context;

    protected TraceRouteSupplier() {
        this(TraceContext.capture());
    }

    protected TraceRouteSupplier(TraceContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public final T get() {
        try (TraceContext.Scope ignored = context.open()) {
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
