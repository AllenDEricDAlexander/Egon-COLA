package top.egon.cola.component.ddc.trace;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceIds;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceSnapshot;
import top.egon.cola.component.common.trace.TraceState;

public final class DdcTraceSupport {

    private static final String COMPONENT_KEY = "component";

    private static final String OPERATION_KEY = "operation";

    private DdcTraceSupport() {
    }

    public static void inject(HttpHeaders headers) {
        TraceState child = TraceContext.current()
                .orElseGet(() -> TraceState.root(TraceIds.newTraceId()))
                .child();
        TracePropagation.inject(child, headers::set);
    }

    public static TraceSnapshot captureOrCreate() {
        return TraceContext.current()
                .map(ignored -> TraceContext.snapshot())
                .orElseGet(() -> {
                    TraceState state = TraceState.root(TraceIds.newTraceId());
                    return new TraceSnapshot(state, state.toMdcMap());
                });
    }

    public static Scope openOperation(String operation) {
        TraceState state = TraceContext.current()
                .orElseGet(() -> TraceState.root(TraceIds.newTraceId()));
        return open(state, operation);
    }

    public static Scope openSnapshot(TraceSnapshot snapshot,
                                     String operation) {
        if (snapshot == null || snapshot.state() == null) {
            return openOperation(operation);
        }
        return new Scope(snapshot.open(), operation);
    }

    public static Runnable wrapNewOperation(String operation,
                                            Runnable runnable) {
        return () -> {
            try (Scope ignored = openOperation(operation)) {
                runnable.run();
            }
        };
    }

    public static Runnable wrapSnapshot(TraceSnapshot snapshot,
                                        String operation,
                                        Runnable runnable) {
        return () -> {
            try (Scope ignored = openSnapshot(snapshot, operation)) {
                runnable.run();
            }
        };
    }

    private static Scope open(TraceState state, String operation) {
        return new Scope(TraceContext.open(state), operation);
    }

    public static final class Scope implements AutoCloseable {

        private final TraceScope traceScope;

        private final String previousComponent;

        private final String previousOperation;

        private Scope(TraceScope traceScope, String operation) {
            this.traceScope = traceScope;
            this.previousComponent = MDC.get(COMPONENT_KEY);
            this.previousOperation = MDC.get(OPERATION_KEY);
            MDC.put(COMPONENT_KEY, "ddc");
            if (operation == null || operation.isBlank()) {
                MDC.remove(OPERATION_KEY);
            } else {
                MDC.put(OPERATION_KEY, operation);
            }
        }

        @Override
        public void close() {
            restore(OPERATION_KEY, previousOperation);
            restore(COMPONENT_KEY, previousComponent);
            traceScope.close();
        }

        private void restore(String key, String value) {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }
    }
}
