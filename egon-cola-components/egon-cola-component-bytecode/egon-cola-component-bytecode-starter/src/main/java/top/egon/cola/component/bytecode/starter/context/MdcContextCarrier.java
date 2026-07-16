package top.egon.cola.component.bytecode.starter.context;

import org.slf4j.MDC;
import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.Map;

public final class MdcContextCarrier implements ContextCarrier {

    @Override
    public String name() {
        return "mdc";
    }

    @Override
    public Object capture() {
        return MDC.getCopyOfContextMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContextScope restore(Object snapshot) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        replace((Map<String, String>) snapshot);
        return () -> replace(previous);
    }

    private void replace(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
