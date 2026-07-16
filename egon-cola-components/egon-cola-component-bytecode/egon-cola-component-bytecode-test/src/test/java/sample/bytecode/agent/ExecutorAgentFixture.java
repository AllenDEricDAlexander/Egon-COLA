package sample.bytecode.agent;

import org.slf4j.MDC;
import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;
import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.starter.context.MdcContextCarrier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutorAgentFixture {

    private static final ThreadLocal<String> CUSTOM_CONTEXT = new ThreadLocal<>();

    private ExecutorAgentFixture() {
    }

    public static void main(String[] arguments) throws Exception {
        verifiesDispatcherUnavailablePath();
        List<ExecutorEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        ExecutorTaskDecorator decorator = new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of(
                        new MdcContextCarrier(),
                        new CustomContextCarrier())),
                new RuntimeEventFanout(List.of(events::add), new BoundedFailureStore(8)),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
        var registration = DispatcherRegistry.register(
                ExecutorAgentFixture.class.getClassLoader(),
                "fork-test",
                new DefaultBytecodeRuntimeDispatcher(decorator)
        );
        try {
            verifiesAllFourCallSitesAndFutureIdentity();
            verifiesContextAndCleanup();
            verifiesFailureRejectionCancellationAndInterrupts();
            verifiesConcurrencyNestedTasksVirtualThreadsAndDtpDeduplication();
            require(events.stream().anyMatch(event -> "SUBMITTED".equals(event.phase())),
                    "submission event missing");
            require(events.stream().anyMatch(event -> "COMPLETED".equals(event.phase())),
                    "completion event missing");
            require(events.stream().anyMatch(event -> "FAILED".equals(event.phase())),
                    "failure event missing");
            require(events.stream().anyMatch(event -> "REJECTED".equals(event.phase())),
                    "rejection event missing");
        } finally {
            registration.close();
            MDC.clear();
            CUSTOM_CONTEXT.remove();
        }
        verifiesDispatcherUnavailablePath();
        System.out.println("EXECUTOR_AGENT_OK");
    }

    public static void execute(Executor executor, Runnable task) {
        executor.execute(task);
    }

    public static Future<?> submitRunnable(ExecutorService executor, Runnable task) {
        return executor.submit(task);
    }

    public static <T> Future<T> submitResult(
            ExecutorService executor, Runnable task, T result) {
        return executor.submit(task, result);
    }

    public static <T> Future<T> submitCallable(
            ExecutorService executor, Callable<T> task) {
        return executor.submit(task);
    }

    private static void verifiesDispatcherUnavailablePath() {
        AtomicInteger calls = new AtomicInteger();
        execute(Runnable::run, calls::incrementAndGet);
        require(calls.get() == 1, "dispatcher-unavailable execution changed");
    }

    private static void verifiesAllFourCallSitesAndFutureIdentity() throws Exception {
        IdentityExecutor executor = new IdentityExecutor();
        Future<?> runnableFuture = submitRunnable(executor, () -> { });
        require(runnableFuture == executor.future, "Runnable Future identity changed");
        require(executor.submissions.get() == 1, "Runnable submitted more than once");
        require(executor.submitted != null, "decorated Runnable missing");

        try (ExecutorService real = Executors.newSingleThreadExecutor()) {
            AtomicInteger calls = new AtomicInteger();
            execute(real, calls::incrementAndGet);
            submitRunnable(real, calls::incrementAndGet).get(10, TimeUnit.SECONDS);
            String result = submitResult(real, calls::incrementAndGet, "result")
                    .get(10, TimeUnit.SECONDS);
            int callable = submitCallable(real, () -> {
                calls.incrementAndGet();
                return 7;
            }).get(10, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (calls.get() < 4 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            require("result".equals(result), "Runnable result changed");
            require(callable == 7, "Callable result changed");
            require(calls.get() == 4, "approved call sites changed invocation count");
        }
    }

    private static void verifiesContextAndCleanup() throws Exception {
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            top.egon.cola.component.dtp.context.UninstrumentedSubmitter
                    .submit(worker, () -> {
                        MDC.put("traceId", "worker");
                        CUSTOM_CONTEXT.set("worker");
                        return null;
                    }).get(10, TimeUnit.SECONDS);
            MDC.put("traceId", "submitter");
            CUSTOM_CONTEXT.set("submitter");

            submitCallable(worker, () -> {
                require("submitter".equals(MDC.get("traceId")), "MDC was not propagated");
                require("submitter".equals(CUSTOM_CONTEXT.get()),
                        "custom context was not propagated");
                MDC.put("traceId", "business");
                CUSTOM_CONTEXT.set("business");
                return null;
            }).get(10, TimeUnit.SECONDS);

            String restored = top.egon.cola.component.dtp.context.UninstrumentedSubmitter
                    .submit(worker, () -> MDC.get("traceId") + ":" + CUSTOM_CONTEXT.get())
                    .get(10, TimeUnit.SECONDS);
            require("worker:worker".equals(restored), "worker context leaked: " + restored);
        }
    }

    private static void verifiesFailureRejectionCancellationAndInterrupts() throws Exception {
        RuntimeException businessFailure = new RuntimeException("business");
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> failed = submitCallable(executor, () -> {
                throw businessFailure;
            });
            try {
                failed.get(10, TimeUnit.SECONDS);
                throw new AssertionError("business failure was swallowed");
            } catch (ExecutionException exception) {
                require(exception.getCause() == businessFailure,
                        "business failure identity changed");
            }

            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch interrupted = new CountDownLatch(1);
            Future<?> cancelled = submitRunnable(executor, () -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException exception) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
            });
            require(started.await(10, TimeUnit.SECONDS), "cancellable task did not start");
            require(cancelled.cancel(true), "cancellation result changed");
            require(interrupted.await(10, TimeUnit.SECONDS), "interrupt was not propagated");
        }

        RejectedExecutionException rejection = new RejectedExecutionException("sentinel");
        try {
            execute(new RejectingExecutor(rejection), () -> { });
            throw new AssertionError("rejection was swallowed");
        } catch (RejectedExecutionException exception) {
            require(exception == rejection, "rejection identity changed");
        }
    }

    private static void verifiesConcurrencyNestedTasksVirtualThreadsAndDtpDeduplication()
            throws Exception {
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                futures.add(submitCallable(virtual, () -> 1));
            }
            int total = 0;
            for (Future<Integer> future : futures) {
                total += future.get(10, TimeUnit.SECONDS);
            }
            require(total == 100, "virtual-thread concurrency result changed");
        }

        try (ExecutorService nested = Executors.newFixedThreadPool(2)) {
            int result = submitCallable(nested,
                    () -> submitCallable(nested, () -> 11).get(10, TimeUnit.SECONDS))
                    .get(10, TimeUnit.SECONDS);
            require(result == 11, "nested task result changed");
        }

        IdentityExecutor executor = new IdentityExecutor();
        Runnable dtpTask = new top.egon.cola.component.dtp.context.DtpRunnable();
        submitRunnable(executor, dtpTask);
        require(executor.submitted == dtpTask, "DTP task was double wrapped");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class CustomContextCarrier implements ContextCarrier {

        @Override
        public String name() {
            return "custom";
        }

        @Override
        public Object capture() {
            return CUSTOM_CONTEXT.get();
        }

        @Override
        public ContextScope restore(Object snapshot) {
            String previous = CUSTOM_CONTEXT.get();
            if (snapshot == null) {
                CUSTOM_CONTEXT.remove();
            } else {
                CUSTOM_CONTEXT.set((String) snapshot);
            }
            return () -> {
                if (previous == null) {
                    CUSTOM_CONTEXT.remove();
                } else {
                    CUSTOM_CONTEXT.set(previous);
                }
            };
        }
    }

    private static final class IdentityExecutor extends AbstractExecutorService {

        private final AtomicInteger submissions = new AtomicInteger();
        private final FutureTask<Void> future = new FutureTask<>(() -> null);
        private Runnable submitted;

        @Override
        public Future<?> submit(Runnable task) {
            submissions.incrementAndGet();
            submitted = task;
            return future;
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class RejectingExecutor implements Executor {

        private final RejectedExecutionException rejection;

        private RejectingExecutor(RejectedExecutionException rejection) {
            this.rejection = rejection;
        }

        @Override
        public void execute(Runnable command) {
            throw rejection;
        }
    }
}
