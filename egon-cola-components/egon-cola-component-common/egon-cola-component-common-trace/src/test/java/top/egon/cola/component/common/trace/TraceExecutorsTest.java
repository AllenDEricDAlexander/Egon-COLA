package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceExecutorsTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearOwnedKeys();
        MDC.clear();
    }

    @Test
    void capturesDifferentContextsForEachExecuteAndSubmit() throws Exception {
        try (ExecutorService delegate = Executors.newSingleThreadExecutor()) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);
            TraceState first = TraceState.root("request-1");
            TraceState second = TraceState.root("request-2");
            CompletableFuture<Observation> executed = new CompletableFuture<>();
            CompletableFuture<Observation> submittedRunnableResult =
                    new CompletableFuture<>();
            Future<Observation> submitted;
            Future<?> submittedRunnable;
            Future<String> submittedRunnableWithResult;

            try (TraceScope ignored = TraceContext.open(first)) {
                MDC.put("biz", "first");
                executor.execute(() -> executed.complete(observe()));
            }
            try (TraceScope ignored = TraceContext.open(second)) {
                MDC.put("biz", "second");
                submitted = executor.submit(TraceExecutorsTest::observe);
                submittedRunnable = executor.submit(
                        () -> submittedRunnableResult.complete(observe())
                );
                submittedRunnableWithResult = executor.submit(
                        () -> assertEquals(second, TraceContext.current().orElseThrow()),
                        "done"
                );
            }

            assertEquals(new Observation(first, "first", false),
                    executed.get(5, TimeUnit.SECONDS));
            assertEquals(new Observation(second, "second", false), submitted.get());
            submittedRunnable.get();
            assertEquals(new Observation(second, "second", false),
                    submittedRunnableResult.get());
            assertEquals("done", submittedRunnableWithResult.get());
        }
    }

    @Test
    void propagatesBatchSubmissionContext() throws Exception {
        try (ExecutorService delegate = Executors.newFixedThreadPool(2)) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);
            TraceState state = TraceState.root("request-batch");

            try (TraceScope ignored = TraceContext.open(state)) {
                MDC.put("biz", "batch");
                List<Future<Observation>> futures = executor.invokeAll(List.of(
                        TraceExecutorsTest::observe,
                        TraceExecutorsTest::observe
                ));
                assertEquals(new Observation(state, "batch", false),
                        futures.getFirst().get());
                assertEquals(new Observation(state, "batch", false),
                        futures.getLast().get());

                List<Future<Observation>> timedFutures = executor.invokeAll(
                        List.of(TraceExecutorsTest::observe),
                        5,
                        TimeUnit.SECONDS
                );
                assertEquals(new Observation(state, "batch", false),
                        timedFutures.getFirst().get());

                Observation any = executor.invokeAny(List.of(
                        TraceExecutorsTest::observe,
                        TraceExecutorsTest::observe
                ));
                assertEquals(new Observation(state, "batch", false), any);
                Observation timedAny = executor.invokeAny(
                        List.of(TraceExecutorsTest::observe),
                        5,
                        TimeUnit.SECONDS
                );
                assertEquals(new Observation(state, "batch", false), timedAny);
            }
        }
    }

    @Test
    void restoresReusedWorkerContextAfterTaskFailure() throws Exception {
        try (ExecutorService delegate = Executors.newSingleThreadExecutor()) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);
            TraceState workerState = delegate.submit(() -> {
                TraceContext.setTraceId(TraceIds.newTraceId());
                MDC.put("biz", "worker");
                return TraceContext.current().orElseThrow();
            }).get();
            TraceState submitter = TraceState.root("request-failure");
            Future<Void> failure;

            try (TraceScope ignored = TraceContext.open(submitter)) {
                MDC.put("biz", "submitter");
                failure = executor.submit(() -> {
                    assertEquals(submitter, TraceContext.current().orElseThrow());
                    assertEquals("submitter", MDC.get("biz"));
                    throw new IllegalStateException("expected");
                });
            }

            ExecutionException error = assertThrows(
                    ExecutionException.class,
                    failure::get
            );
            assertEquals("expected", error.getCause().getMessage());
            assertEquals(new Observation(workerState, "worker", false),
                    delegate.submit(TraceExecutorsTest::observe).get());

            delegate.submit(() -> {
                TraceContext.clearOwnedKeys();
                MDC.clear();
            }).get();
        }
    }

    @Test
    void propagatesContextToVirtualThreads() throws Exception {
        try (ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor()) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);
            TraceState state = TraceState.root("request-virtual");
            Future<Observation> result;

            try (TraceScope ignored = TraceContext.open(state)) {
                MDC.put("biz", "virtual");
                result = executor.submit(TraceExecutorsTest::observe);
            }

            assertEquals(new Observation(state, "virtual", true), result.get());
        }
    }

    @Test
    void cancellationInterruptsTaskAndRestoresWorkerContext() throws Exception {
        try (ExecutorService delegate = Executors.newSingleThreadExecutor()) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);
            TraceState submitter = TraceState.root("request-cancel");
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);
            AtomicBoolean interrupted = new AtomicBoolean();
            AtomicReference<Observation> taskContext = new AtomicReference<>();
            Future<Void> future;

            try (TraceScope ignored = TraceContext.open(submitter)) {
                MDC.put("biz", "cancel");
                future = executor.submit(() -> {
                    taskContext.set(observe());
                    started.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException exception) {
                        interrupted.set(true);
                        throw exception;
                    } finally {
                        finished.countDown();
                    }
                    return null;
                });
            }

            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertTrue(future.cancel(true));
                assertTrue(finished.await(5, TimeUnit.SECONDS));
                assertTrue(interrupted.get());
                assertTrue(future.isCancelled());
                assertTrue(future.isDone());
                assertThrows(CancellationException.class, future::get);
                assertEquals(new Observation(submitter, "cancel", false),
                        taskContext.get());
                assertEquals(new Observation(null, null, false),
                        delegate.submit(TraceExecutorsTest::observe).get());
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void rejectsNullAndAvoidsDuplicateDecoration() throws Exception {
        assertThrows(NullPointerException.class,
                () -> TraceExecutors.contextAware(null));

        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = TraceExecutors.contextAware(delegate);
        assertSame(executor, TraceExecutors.contextAware(executor));

        executor.shutdown();
        assertTrue(delegate.isShutdown());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void rejectsNullTasksBeforeDelegation() {
        try (ExecutorService delegate = Executors.newSingleThreadExecutor()) {
            ExecutorService executor = TraceExecutors.contextAware(delegate);

            assertThrows(NullPointerException.class,
                    () -> executor.execute(null));
            assertThrows(NullPointerException.class,
                    () -> executor.submit((Runnable) null));
            assertThrows(NullPointerException.class,
                    () -> executor.invokeAll(Arrays.asList(() -> null, null)));
            assertNull(TraceContext.getTraceId());
        }
    }

    private static Observation observe() {
        return new Observation(
                TraceContext.current().orElse(null),
                MDC.get("biz"),
                Thread.currentThread().isVirtual()
        );
    }

    private record Observation(
            TraceState state,
            String businessMdc,
            boolean virtualThread) {
    }
}
