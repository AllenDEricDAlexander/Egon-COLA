package top.egon.cola.component.dtp.context;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Callable
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpCallable<V> implements Callable<V> {

    private final Callable<V> delegate;

    private final DtpContextSnapshot snapshot;

    private DtpCallable(Callable<V> delegate, DtpContextSnapshot snapshot) {
        this.delegate = delegate;
        this.snapshot = snapshot;
    }

    public static <V> Callable<V> wrap(Callable<V> callable) {
        Objects.requireNonNull(callable, "callable");
        if (callable instanceof DtpCallable<?>) {
            return callable;
        }
        return new DtpCallable<>(callable, DtpContextSnapshot.capture());
    }

    @Override
    public V call() throws Exception {
        try (DtpContextSnapshot.Scope ignored = snapshot.restore()) {
            return delegate.call();
        }
    }

}
