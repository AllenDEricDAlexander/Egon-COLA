package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextScopeTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void keepsLegacyTraceIdAccessorsCompatible() {
        TraceContext.setTraceId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", TraceContext.getTraceId());

        TraceContext.clearTraceId();

        assertNull(TraceContext.getTraceId());
    }

    @Test
    void scopeRestoresNestedTraceKeysWithoutClearingBusinessMdc() {
        MDC.put("biz", "keep");
        MDC.put(TraceKeys.TRACE_ID, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        TraceState outer = TraceState.root();
        TraceState inner = outer.child();

        try (TraceScope ignored = TraceContext.open(outer)) {
            assertEquals(outer.traceId(), MDC.get(TraceKeys.TRACE_ID));
            try (TraceScope nested = TraceContext.open(inner)) {
                assertEquals(inner.spanId(), MDC.get(TraceKeys.SPAN_ID));
                MDC.put("biz", "changed");
            }
            assertEquals(outer.spanId(), MDC.get(TraceKeys.SPAN_ID));
            assertEquals("changed", MDC.get("biz"));
        }

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", MDC.get(TraceKeys.TRACE_ID));
        assertEquals("changed", MDC.get("biz"));
    }

    @Test
    void clearOwnedKeysDoesNotClearForeignMdc() {
        TraceState state = TraceState.root();
        try (TraceScope ignored = TraceContext.open(state)) {
            MDC.put("biz", "keep");
            TraceContext.clearOwnedKeys();
            assertNull(MDC.get(TraceKeys.TRACE_ID));
            assertEquals("keep", MDC.get("biz"));
        }
    }

    @Test
    void snapshotWrapsRunnableCallableSupplierAndRestoresWorkerMdc() throws Exception {
        TraceState submitter = TraceState.root();
        TraceState worker = TraceState.root();
        TraceSnapshot snapshot;
        try (TraceScope ignored = TraceContext.open(submitter)) {
            snapshot = TraceContext.snapshot();
        }
        try (TraceScope ignored = TraceContext.open(worker)) {
            AtomicReference<String> runnableTrace = new AtomicReference<>();
            Runnable runnable = snapshot.wrap(() ->
                    runnableTrace.set(TraceContext.getTraceId())
            );
            Callable<String> callable = snapshot.wrap(
                    (Callable<String>) TraceContext::getTraceId
            );
            Supplier<String> supplier = snapshot.wrap(
                    (Supplier<String>) TraceContext::getTraceId
            );

            runnable.run();

            assertEquals(submitter.traceId(), runnableTrace.get());
            assertEquals(submitter.traceId(), callable.call());
            assertEquals(submitter.traceId(), supplier.get());
            assertEquals(worker.traceId(), TraceContext.getTraceId());
        }
    }

    @Test
    void decoratedExecutorDoesNotLeakAcrossThreadReuse() throws Exception {
        TraceState first = TraceState.root();
        Executor sameThread = Runnable::run;
        Executor decorated;
        try (TraceScope ignored = TraceContext.open(first)) {
            decorated = TraceContext.snapshot().decorate(sameThread);
        }
        TraceContext.clearOwnedKeys();
        FutureTask<String> firstTask = new FutureTask<>(TraceContext::getTraceId);
        decorated.execute(firstTask);

        assertEquals(first.traceId(), firstTask.get());
        assertNull(TraceContext.getTraceId());

        FutureTask<Boolean> secondTask = new FutureTask<>(() ->
                TraceContext.current().isEmpty()
        );
        sameThread.execute(secondTask);

        assertTrue(secondTask.get());
    }
}
