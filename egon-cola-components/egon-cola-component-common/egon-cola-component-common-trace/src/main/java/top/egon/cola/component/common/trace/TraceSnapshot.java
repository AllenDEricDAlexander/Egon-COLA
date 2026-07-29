package top.egon.cola.component.common.trace;

import top.egon.cola.component.common.trace.thread.TraceRouteCallable;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;
import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Immutable snapshot of the current trace context.
 */
public final class TraceSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TraceState state;

    private final Map<String, String> mdcContext;

    public TraceSnapshot(String traceId) {
        this(
                TraceIds.isValidTraceId(traceId)
                        ? new TraceState(
                        traceId.toLowerCase(java.util.Locale.ROOT),
                        TraceIds.newSpanId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
                        : null,
                traceId == null || traceId.isBlank()
                        ? Map.of()
                        : Map.of(TraceKeys.TRACE_ID, traceId)
        );
    }

    public TraceSnapshot(TraceState state, Map<String, String> mdcContext) {
        this.state = state;
        Map<String, String> normalizedContext = new LinkedHashMap<>();
        if (mdcContext != null) {
            normalizedContext.putAll(mdcContext);
        }
        if (state != null) {
            TraceKeys.ownedMdcKeys().forEach(normalizedContext::remove);
            normalizedContext.putAll(state.toMdcMap());
        }
        this.mdcContext = normalizedContext.isEmpty()
                ? Map.of()
                : Map.copyOf(normalizedContext);
    }

    public static TraceSnapshot capture() {
        Map<String, String> mdcContext =
                org.slf4j.MDC.getCopyOfContextMap();
        TraceState state = TraceContext.current()
                .orElseGet(() -> TraceState.fromMdc(mdcContext).orElse(null));
        return new TraceSnapshot(state, mdcContext);
    }

    public String getTraceId() {
        if (state != null) {
            return state.traceId();
        }
        return mdcContext.get(TraceKeys.TRACE_ID);
    }

    public TraceState state() {
        return state;
    }

    public Map<String, String> mdcContext() {
        return mdcContext;
    }

    public TraceScope open() {
        return TraceScope.open(this);
    }

    public Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return new TraceRouteRunnable(this) {
            @Override
            protected void doRun() {
                runnable.run();
            }
        };
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable");
        return new TraceRouteCallable<>(this) {
            @Override
            protected T doCall() throws Exception {
                return callable.call();
            }
        };
    }

    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return new TraceRouteSupplier<>(this) {
            @Override
            protected T doGet() {
                return supplier.get();
            }
        };
    }

    public Executor decorate(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return command -> executor.execute(wrap(command));
    }
}
