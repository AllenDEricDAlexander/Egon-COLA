package top.egon.cola.component.dtp.context;

import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Supplier
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpSupplier<T> extends TraceRouteSupplier<T> {

    private final TraceContext context;

    private final Supplier<T> delegate;

    private DtpSupplier(Supplier<T> delegate) {
        this(delegate, TraceContext.capture());
    }

    private DtpSupplier(Supplier<T> delegate, TraceContext context) {
        super(context);
        this.context = context;
        this.delegate = delegate;
    }

    TraceContext context() {
        return context;
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (supplier instanceof DtpSupplier<?>) {
            return supplier;
        }
        return new DtpSupplier<>(supplier);
    }

    @Override
    protected T doGet() {
        DtpTaskObserver.effectiveContext();
        try {
            T result = delegate.get();
            DtpTaskObserver.wrappedOutcome(null);
            return result;
        } catch (RuntimeException | Error e) {
            DtpTaskObserver.wrappedOutcome(e);
            throw e;
        }
    }
}
