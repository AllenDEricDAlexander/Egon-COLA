package top.egon.cola.component.common.trace.autoconfigure;

import io.micrometer.context.ThreadLocalAccessor;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;

public class TraceThreadLocalAccessor
        implements ThreadLocalAccessor<TraceSnapshot> {

    private static final String KEY =
            "top.egon.cola.component.common.trace.TraceSnapshot";

    private final ThreadLocal<TraceScope> activeScope = new ThreadLocal<>();

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public TraceSnapshot getValue() {
        return TraceContext.snapshot();
    }

    @Override
    public void setValue(TraceSnapshot value) {
        closeActiveScope();
        if (value != null) {
            activeScope.set(value.open());
        }
    }

    @Override
    public void setValue() {
        closeActiveScope();
        TraceContext.clearOwnedKeys();
    }

    @Override
    public void reset() {
        closeActiveScope();
    }

    private void closeActiveScope() {
        TraceScope scope = activeScope.get();
        if (scope != null) {
            scope.close();
            activeScope.remove();
        }
    }
}
