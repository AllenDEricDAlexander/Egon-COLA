package top.egon.cola.component.common.trace.thread;

import top.egon.cola.component.common.trace.TraceContext;

import java.util.Objects;

/**
 * Runnable template that restores the trace context captured at construction.
 */
public abstract class TraceRouteRunnable implements Runnable {

    private final TraceContext context;

    protected TraceRouteRunnable() {
        this(TraceContext.capture());
    }

    protected TraceRouteRunnable(TraceContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public final void run() {
        try (TraceContext.Scope ignored = context.open()) {
            doRun();
        }
    }

    /**
     * Executes the task under the captured trace context.
     */
    protected abstract void doRun();
}
