package top.egon.cola.component.ddc.trace;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import top.egon.cola.component.common.trace.TraceContext;

public final class DdcTraceSupport {

    private static final String COMPONENT_KEY = "component";

    private static final String OPERATION_KEY = "operation";

    private DdcTraceSupport() {
    }

    public static void inject(HttpHeaders headers) {
        TraceContext.currentOrCreate().child().inject(headers::set);
    }

    public static TraceContext captureOrCreate() {
        return TraceContext.currentOrCreate();
    }

    public static Scope openOperation(String operation) {
        return new Scope(
                TraceContext.currentOrCreate().open(),
                operation
        );
    }

    public static Scope openContext(TraceContext context,
                                    String operation) {
        return new Scope(context.open(), operation);
    }

    public static Runnable wrapNewOperation(String operation,
                                            Runnable runnable) {
        return () -> {
            try (Scope ignored = openOperation(operation)) {
                runnable.run();
            }
        };
    }

    public static Runnable wrapContext(TraceContext context,
                                       String operation,
                                       Runnable runnable) {
        return () -> {
            try (Scope ignored = openContext(context, operation)) {
                runnable.run();
            }
        };
    }

    private static void restore(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    public static final class Scope implements AutoCloseable {

        private final TraceContext.Scope traceScope;

        private final String previousComponent;

        private final String previousOperation;

        private Scope(TraceContext.Scope traceScope, String operation) {
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
    }
}
