package top.egon.cola.component.bytecode.starter.context;

import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;
import top.egon.cola.component.common.trace.TraceContext;

public final class MdcContextCarrier implements ContextCarrier {

    @Override
    public String name() {
        return "mdc";
    }

    @Override
    public Object capture() {
        return TraceContext.capture();
    }

    @Override
    public ContextScope restore(Object snapshot) {
        TraceContext.Scope scope = ((TraceContext) snapshot).open();
        return scope::close;
    }
}
