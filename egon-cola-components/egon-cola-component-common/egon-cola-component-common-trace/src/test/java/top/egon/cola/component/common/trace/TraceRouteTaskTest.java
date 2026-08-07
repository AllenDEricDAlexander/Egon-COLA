package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.thread.TraceRouteCallable;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;
import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceRouteTaskTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void wrappersCaptureCompleteMdcAndRestoreWorker() throws Exception {
        AtomicReference<String> runnableTrace = new AtomicReference<>();
        TraceContext context = TraceContext.root("request-1");
        TraceRouteRunnable runnable;
        TraceRouteCallable<String> callable;
        TraceRouteSupplier<String> supplier;
        try (TraceContext.Scope ignored = context.open()) {
            MDC.put("tenantId", "tenant-1");
            runnable = new TraceRouteRunnable() {
                @Override
                protected void doRun() {
                    runnableTrace.set(
                            TraceContext.getTraceId() + ":" + MDC.get("tenantId")
                    );
                }
            };
            callable = new TraceRouteCallable<>() {
                @Override
                protected String doCall() {
                    return TraceContext.getTraceId() + ":" + MDC.get("tenantId");
                }
            };
            supplier = new TraceRouteSupplier<>() {
                @Override
                protected String doGet() {
                    return TraceContext.getTraceId() + ":" + MDC.get("tenantId");
                }
            };
        }
        MDC.put(TraceContext.TRACE_ID, "worker-trace");

        runnable.run();
        String expected = context.traceId() + ":tenant-1";

        assertEquals(expected, runnableTrace.get());
        assertEquals(expected, callable.call());
        assertEquals(expected, supplier.get());
        assertEquals("worker-trace", TraceContext.getTraceId());
    }

    @Test
    void wrapperRestoresWorkerAfterFailure() {
        TraceRouteRunnable runnable = new TraceRouteRunnable() {
            @Override
            protected void doRun() {
                throw new IllegalStateException("failed");
            }
        };
        MDC.put("workerKey", "worker-value");

        assertThrows(IllegalStateException.class, runnable::run);
        assertEquals("worker-value", MDC.get("workerKey"));
    }
}
