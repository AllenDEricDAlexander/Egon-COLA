package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearOwnedKeys();
        MDC.clear();
    }

    @Test
    void capturesContextForEachDecoratedTaskAndRestoresWorker() throws Exception {
        TraceTaskDecorator decorator = new TraceTaskDecorator();
        TraceState first = TraceState.root("request-1");
        TraceState second = TraceState.root("request-2");
        AtomicReference<Observation> firstResult = new AtomicReference<>();
        AtomicReference<Observation> secondResult = new AtomicReference<>();
        Runnable firstTask;
        Runnable secondTask;

        try (TraceScope ignored = TraceContext.open(first)) {
            MDC.put("biz", "first");
            firstTask = decorator.decorate(() -> firstResult.set(observe()));
        }
        try (TraceScope ignored = TraceContext.open(second)) {
            MDC.put("biz", "second");
            secondTask = decorator.decorate(() -> secondResult.set(observe()));
        }
        TraceContext.clearOwnedKeys();
        MDC.clear();

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(firstTask).get();
            executor.submit(secondTask).get();

            assertEquals(new Observation(first, "first"), firstResult.get());
            assertEquals(new Observation(second, "second"), secondResult.get());
            assertNull(executor.submit(TraceContext::getTraceId).get());
            assertNull(executor.submit(() -> MDC.get("biz")).get());
        }
    }

    @Test
    void rejectsNullTask() {
        TraceTaskDecorator decorator = new TraceTaskDecorator();

        assertThrows(NullPointerException.class,
                () -> decorator.decorate(null));
    }

    private static Observation observe() {
        return new Observation(
                TraceContext.current().orElse(null),
                MDC.get("biz")
        );
    }

    private record Observation(TraceState state, String businessMdc) {
    }
}
