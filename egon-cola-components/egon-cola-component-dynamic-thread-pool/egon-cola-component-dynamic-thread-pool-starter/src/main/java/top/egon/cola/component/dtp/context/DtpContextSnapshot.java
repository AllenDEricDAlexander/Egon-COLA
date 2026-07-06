package top.egon.cola.component.dtp.context;

import org.slf4j.MDC;

import java.util.Map;

/**
 * @author      有罗敷的马同学
 * @description DTP 线程上下文快照
 * @Date        下午9:27 2026/6/29
 **/
public final class DtpContextSnapshot {

    private final Map<String, String> mdcContext;

    private DtpContextSnapshot(Map<String, String> mdcContext) {
        this.mdcContext = mdcContext;
    }

    public static DtpContextSnapshot capture() {
        return new DtpContextSnapshot(MDC.getCopyOfContextMap());
    }

    public Scope restore() {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (mdcContext == null || mdcContext.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdcContext);
        }
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {

        private final Map<String, String> previous;

        private Scope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }

    }

}
