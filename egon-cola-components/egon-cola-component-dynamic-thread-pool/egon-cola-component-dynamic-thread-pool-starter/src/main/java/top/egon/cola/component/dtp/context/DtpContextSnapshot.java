package top.egon.cola.component.dtp.context;

import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;

/**
 * @author      有罗敷的马同学
 * @description DTP 线程上下文快照
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpContextSnapshot {

    private final TraceSnapshot traceSnapshot;

    private DtpContextSnapshot(TraceSnapshot traceSnapshot) {
        this.traceSnapshot = traceSnapshot;
    }

    public static DtpContextSnapshot capture() {
        return new DtpContextSnapshot(TraceSnapshot.capture());
    }

    public Scope restore() {
        return new Scope(traceSnapshot.open());
    }

    public static final class Scope implements AutoCloseable {

        private final TraceScope delegate;

        private Scope(TraceScope delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            delegate.close();
        }

    }

}
