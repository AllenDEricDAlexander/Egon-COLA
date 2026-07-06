package top.egon.cola.component.dtp.context;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Supplier
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;

    private final DtpContextSnapshot snapshot;

    private DtpSupplier(Supplier<T> delegate, DtpContextSnapshot snapshot) {
        this.delegate = delegate;
        this.snapshot = snapshot;
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (supplier instanceof DtpSupplier<?>) {
            return supplier;
        }
        return new DtpSupplier<>(supplier, DtpContextSnapshot.capture());
    }

    @Override
    public T get() {
        try (DtpContextSnapshot.Scope ignored = snapshot.restore()) {
            return delegate.get();
        }
    }

}
