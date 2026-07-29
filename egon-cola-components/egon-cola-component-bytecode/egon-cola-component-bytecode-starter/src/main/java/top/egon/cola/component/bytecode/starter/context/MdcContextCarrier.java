package top.egon.cola.component.bytecode.starter.context;

import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;

import java.util.Map;

public final class MdcContextCarrier implements ContextCarrier {

    @Override
    public String name() {
        return "mdc";
    }

    @Override
    public Object capture() {
        return TraceSnapshot.capture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContextScope restore(Object snapshot) {
        TraceSnapshot traceSnapshot = snapshot instanceof TraceSnapshot current
                ? current
                : new TraceSnapshot(null, (Map<String, String>) snapshot);
        TraceScope scope = traceSnapshot.open();
        return scope::close;
    }
}
