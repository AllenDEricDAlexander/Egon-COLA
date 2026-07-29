package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceRouteTaskTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        TraceContext.clearOwnedKeys();
    }

    @Test
    void runnablePropagatesCapturedContextToVirtualThread() throws Exception {
        TraceState parent = TraceState.root();
        AtomicReference<String> actualTraceId = new AtomicReference<>();
        TraceRouteRunnable task;
        try (TraceScope ignored = TraceContext.open(parent)) {
            MDC.put("biz", "order");
            task = new TraceRouteRunnable() {
                @Override
                protected void doRun() {
                    actualTraceId.set(TraceContext.getTraceId());
                    assertEquals("order", MDC.get("biz"));
                }
            };
        }

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(task).get();
        }

        assertEquals(parent.traceId(), actualTraceId.get());
        assertNull(TraceContext.getTraceId());
        assertEquals("order", MDC.get("biz"));
    }

    @Test
    void callableReturnsResultAndRestoresWorkerContext() throws Exception {
        TraceState parent = TraceState.root();
        TraceRouteCallable<String> task;
        try (TraceScope ignored = TraceContext.open(parent)) {
            task = new TraceRouteCallable<>() {
                @Override
                protected String doCall() {
                    return TraceContext.getTraceId();
                }
            };
        }

        TraceState worker = TraceState.root();
        try (TraceScope ignored = TraceContext.open(worker)) {
            assertEquals(parent.traceId(), task.call());
            assertEquals(worker.traceId(), TraceContext.getTraceId());
        }
    }

    @Test
    void callablePreservesCheckedExceptionAndRestoresContext() {
        TraceState parent = TraceState.root();
        TraceRouteCallable<Void> task;
        try (TraceScope ignored = TraceContext.open(parent)) {
            task = new TraceRouteCallable<>() {
                @Override
                protected Void doCall() throws Exception {
                    throw new Exception("expected");
                }
            };
        }

        TraceState worker = TraceState.root();
        try (TraceScope ignored = TraceContext.open(worker)) {
            Exception error = assertThrows(Exception.class, task::call);
            assertEquals("expected", error.getMessage());
            assertEquals(worker.traceId(), TraceContext.getTraceId());
        }
    }

    @Test
    void supplierReturnsResultAndRestoresContextAfterFailure() {
        TraceState parent = TraceState.root();
        TraceRouteSupplier<String> success;
        TraceRouteSupplier<String> failure;
        try (TraceScope ignored = TraceContext.open(parent)) {
            success = new TraceRouteSupplier<>() {
                @Override
                protected String doGet() {
                    return TraceContext.getTraceId();
                }
            };
            failure = new TraceRouteSupplier<>() {
                @Override
                protected String doGet() {
                    throw new IllegalStateException("expected");
                }
            };
        }

        TraceState worker = TraceState.root();
        try (TraceScope ignored = TraceContext.open(worker)) {
            assertEquals(parent.traceId(), success.get());
            assertThrows(IllegalStateException.class, failure::get);
            assertEquals(worker.traceId(), TraceContext.getTraceId());
        }
    }
}
