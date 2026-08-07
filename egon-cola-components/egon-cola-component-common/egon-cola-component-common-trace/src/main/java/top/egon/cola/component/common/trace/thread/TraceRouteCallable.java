package top.egon.cola.component.common.trace.thread;

import top.egon.cola.component.common.trace.TraceContext;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Callable template that restores the trace context captured at construction.
 *
 * @param <T> task result type
 */
public abstract class TraceRouteCallable<T> implements Callable<T> {

    private final TraceContext context;

    protected TraceRouteCallable() {
        this(TraceContext.capture());
    }

    protected TraceRouteCallable(TraceContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public final T call() throws Exception {
        try (TraceContext.Scope ignored = context.open()) {
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
