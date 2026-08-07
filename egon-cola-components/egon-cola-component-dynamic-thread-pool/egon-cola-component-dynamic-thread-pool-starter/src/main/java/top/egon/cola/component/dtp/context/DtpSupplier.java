package top.egon.cola.component.dtp.context;

import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Supplier
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpSupplier<T> extends TraceRouteSupplier<T> {

    private final Supplier<T> delegate;

    private DtpSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
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
        return delegate.get();
    }
}
