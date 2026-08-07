package top.egon.cola.component.common.trace.autoconfigure;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import top.egon.cola.component.common.trace.TraceContext;

public class TraceThreadLocalAccessor
        implements ThreadLocalAccessor<TraceContext>, AutoCloseable {

    public TraceThreadLocalAccessor() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(this);
    }

    @Override
    public Object key() {
        return TraceContext.class;
    }

    @Override
    public TraceContext getValue() {
        TraceContext context = TraceContext.capture();
        return context.mdcContext().isEmpty() ? null : context;
    }

    @Override
    public void setValue(TraceContext value) {
        TraceContext.install(value);
    }

    @Override
    public void setValue() {
        TraceContext.clear();
    }

    @Override
    public void close() {
        ContextRegistry.getInstance().removeThreadLocalAccessor(
                TraceContext.class
        );
    }
}
