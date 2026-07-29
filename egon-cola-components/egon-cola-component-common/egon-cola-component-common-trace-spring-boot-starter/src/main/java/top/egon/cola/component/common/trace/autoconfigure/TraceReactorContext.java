package top.egon.cola.component.common.trace.autoconfigure;

import reactor.util.context.Context;
import reactor.util.context.ContextView;
import top.egon.cola.component.common.trace.TraceState;

public final class TraceReactorContext {

    public static final String TRACE_STATE_KEY =
            "top.egon.cola.component.common.trace.TraceState";

    private TraceReactorContext() {
    }

    public static Context put(Context context, TraceState state) {
        return context.put(TRACE_STATE_KEY, state);
    }

    public static TraceState get(ContextView contextView) {
        return contextView.getOrDefault(TRACE_STATE_KEY, null);
    }
}
