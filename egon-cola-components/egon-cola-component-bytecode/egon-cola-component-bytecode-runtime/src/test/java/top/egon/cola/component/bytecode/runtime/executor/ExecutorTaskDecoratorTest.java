package top.egon.cola.component.bytecode.runtime.executor;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;
import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorTaskDecoratorTest {

    private static final Executor DIRECT = Runnable::run;

    @Test
    void capturesOnceRestoresForExecutionAndPublishesLifecycle() {
        AtomicInteger captures = new AtomicInteger();
        AtomicReference<String> context = new AtomicReference<>("submitter");
        List<ExecutorEvent> events = new ArrayList<>();
        ExecutorTaskDecorator decorator = decorator(
                new ContextCarrier() {
                    @Override
                    public String name() {
                        return "test";
                    }

                    @Override
                    public Object capture() {
                        captures.incrementAndGet();
                        return context.get();
                    }

                    @Override
                    public ContextScope restore(Object snapshot) {
                        String previous = context.getAndSet((String) snapshot);
                        return () -> context.set(previous);
                    }
                }, events);

        Runnable task = decorator.decorateRunnable(DIRECT, () -> {
            assertEquals("submitter", context.get());
            context.set("business");
        }, 7L);
        context.set("worker");
        task.run();

        assertEquals(1, captures.get());
        assertEquals("worker", context.get());
        assertTrue(task instanceof EgonInstrumentedTask);
        assertEquals(List.of("SUBMITTED", "STARTED", "COMPLETED"),
                events.stream().map(ExecutorEvent::phase).toList());
        assertEquals("SUCCESS", events.getLast().result());
    }

    @Test
    void preservesExactCallableFailureAndInterruptState() {
        ExecutorTaskDecorator decorator = decorator(noopCarrier(), new ArrayList<>());
        RuntimeException failure = new RuntimeException("business");
        Callable<String> task = decorator.decorateCallable(DIRECT, () -> {
            Thread.currentThread().interrupt();
            throw failure;
        }, 8L);

        RuntimeException actual = assertThrows(RuntimeException.class, task::call);

        assertSame(failure, actual);
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void sinkFailureIsIsolatedAndRecorded() {
        BoundedFailureStore failures = new BoundedFailureStore(2);
        RuntimeEventFanout fanout = new RuntimeEventFanout(
                List.of(event -> {
                    throw new IllegalStateException("sink failed");
                }), failures);
        ExecutorTaskDecorator decorator = new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of(noopCarrier())),
                fanout,
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
        AtomicInteger calls = new AtomicInteger();

        decorator.decorateRunnable(DIRECT, calls::incrementAndGet, 9L).run();

        assertEquals(1, calls.get());
        assertEquals(2, failures.failures().size());
    }

    @Test
    void avoidsDoubleWrappingEgonAndDtpTasks() {
        ExecutorTaskDecorator decorator = decorator(noopCarrier(), new ArrayList<>());
        Runnable first = decorator.decorateRunnable(DIRECT, () -> { }, 10L);
        Runnable second = decorator.decorateRunnable(DIRECT, first, 11L);
        Runnable dtp = new top.egon.cola.component.dtp.context.DtpRunnable();

        assertSame(first, second);
        assertSame(dtp, decorator.decorateRunnable(DIRECT, dtp, 12L));
    }

    @Test
    void supportsConcurrentDecoratedTasksWithoutSharingSnapshots() throws Exception {
        AtomicInteger captures = new AtomicInteger();
        ExecutorTaskDecorator decorator = decorator(new ContextCarrier() {
            @Override
            public String name() {
                return "counter";
            }

            @Override
            public Object capture() {
                return captures.incrementAndGet();
            }

            @Override
            public ContextScope restore(Object snapshot) {
                return () -> { };
            }
        }, new ArrayList<>());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                tasks.add(decorator.decorateCallable(executor, () -> 1, index));
            }
            int sum = executor.invokeAll(tasks).stream()
                    .mapToInt(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .sum();
            assertEquals(100, sum);
            assertEquals(100, captures.get());
        }
    }

    private ExecutorTaskDecorator decorator(ContextCarrier carrier, List<ExecutorEvent> events) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of(carrier)),
                new RuntimeEventFanout(List.of(events::add), new BoundedFailureStore(10)),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }

    private ContextCarrier noopCarrier() {
        return new ContextCarrier() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public Object capture() {
                return null;
            }

            @Override
            public ContextScope restore(Object snapshot) {
                return () -> { };
            }
        };
    }
}
