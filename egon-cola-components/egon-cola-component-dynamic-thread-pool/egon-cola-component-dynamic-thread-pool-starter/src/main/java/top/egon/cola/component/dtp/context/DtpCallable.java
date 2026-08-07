package top.egon.cola.component.dtp.context;

import top.egon.cola.component.common.trace.thread.TraceRouteCallable;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Callable
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpCallable<V> extends TraceRouteCallable<V> {

    private final Callable<V> delegate;

    private DtpCallable(Callable<V> delegate) {
        this.delegate = delegate;
    }

    public static <V> Callable<V> wrap(Callable<V> callable) {
        Objects.requireNonNull(callable, "callable");
        if (callable instanceof DtpCallable<?>) {
            return callable;
        }
        return new DtpCallable<>(callable);
    }

    @Override
    protected V doCall() throws Exception {
        return delegate.call();
    }
}
