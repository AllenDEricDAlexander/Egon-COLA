package top.egon.cola.component.dtp.context;

import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;

import java.util.Objects;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Runnable
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpRunnable extends TraceRouteRunnable {

    private final TraceContext context;

    private final Runnable delegate;

    private DtpRunnable(Runnable delegate) {
        this(delegate, TraceContext.capture());
    }

    private DtpRunnable(Runnable delegate, TraceContext context) {
        super(context);
        this.context = context;
        this.delegate = delegate;
    }

    TraceContext context() {
        return context;
    }

    public static Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (runnable instanceof DtpRunnable) {
            return runnable;
        }
        return new DtpRunnable(runnable);
    }

    @Override
    protected void doRun() {
        DtpTaskObserver.effectiveContext();
        try {
            delegate.run();
            DtpTaskObserver.wrappedOutcome(null);
        } catch (RuntimeException | Error e) {
            DtpTaskObserver.wrappedOutcome(e);
            throw e;
        }
    }
}
