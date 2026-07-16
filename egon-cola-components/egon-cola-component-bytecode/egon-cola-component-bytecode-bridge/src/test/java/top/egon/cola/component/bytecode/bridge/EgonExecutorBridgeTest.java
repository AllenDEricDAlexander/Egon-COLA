package top.egon.cola.component.bytecode.bridge;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonExecutorBridgeTest {

    @Test
    void returnsExactFutureFromEverySubmitOverload() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        try (DispatcherRegistration ignored = DispatcherRegistry.register(
                Caller.class.getClassLoader(), "runtime", new PassThroughDispatcher())) {
            Future<String> callable = EgonExecutorBridge.submit(
                    executor, () -> "ok", Caller.class, 42L);
            assertSame(executor.lastFuture(), callable);
            assertEquals("ok", callable.get());

            Future<?> runnable = EgonExecutorBridge.submit(
                    executor, () -> { }, Caller.class, 43L);
            assertSame(executor.lastFuture(), runnable);

            Future<String> withResult = EgonExecutorBridge.submit(
                    executor, () -> { }, "result", Caller.class, 44L);
            assertSame(executor.lastFuture(), withResult);
            assertEquals("result", withResult.get());
        }
    }

    @Test
    void decorationFailureFallsBackToOriginalTask() {
        RecordingExecutor executor = new RecordingExecutor();
        AtomicBoolean ran = new AtomicBoolean();
        try (DispatcherRegistration ignored = DispatcherRegistry.register(
                Caller.class.getClassLoader(), "runtime", new FailingDispatcher())) {
            EgonExecutorBridge.execute(executor, () -> ran.set(true), Caller.class, 1L);
        }
        assertTrue(ran.get());
    }

    @Test
    void rethrowsExactRejectionAndPublishesBestEffortCallback() {
        RejectedExecutionException rejection = new RejectedExecutionException("full");
        AtomicReference<RejectedExecutionException> reported = new AtomicReference<>();
        Executor rejecting = task -> {
            throw rejection;
        };
        PassThroughDispatcher dispatcher = new PassThroughDispatcher() {
            @Override
            public void executorRejected(
                    Class<?> callerClass,
                    long callSiteId,
                    Executor executor,
                    RejectedExecutionException exception
            ) {
                reported.set(exception);
            }
        };
        try (DispatcherRegistration ignored = DispatcherRegistry.register(
                Caller.class.getClassLoader(), "runtime", dispatcher)) {
            RejectedExecutionException actual = assertThrows(RejectedExecutionException.class,
                    () -> EgonExecutorBridge.execute(
                            rejecting, () -> { }, Caller.class, 9L));
            assertSame(rejection, actual);
            assertSame(rejection, reported.get());
        }
    }

    private static final class Caller {
    }

    private static class PassThroughDispatcher implements BytecodeRuntimeDispatcher {

        @Override
        public int protocolMajor() {
            return BridgeProtocol.MAJOR;
        }

        @Override
        public int protocolMinor() {
            return BridgeProtocol.MINOR;
        }

        @Override
        public Set<BridgeCapability> capabilities() {
            return Set.of(BridgeCapability.EXECUTOR);
        }

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass,
                Executor executor,
                Runnable task,
                long callSiteId
        ) {
            return task;
        }

        @Override
        public <V> Callable<V> decorateCallable(
                Class<?> callerClass,
                Executor executor,
                Callable<V> task,
                long callSiteId
        ) {
            return task;
        }
    }

    private static final class FailingDispatcher extends PassThroughDispatcher {

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass,
                Executor executor,
                Runnable task,
                long callSiteId
        ) {
            throw new IllegalStateException("runtime unavailable");
        }
    }

    private static final class RecordingExecutor extends AbstractExecutorService {

        private volatile boolean shutdown;
        private Future<?> lastFuture;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            if (command instanceof Future<?> future) {
                lastFuture = future;
            }
            command.run();
        }

        Future<?> lastFuture() {
            return lastFuture;
        }
    }
}
