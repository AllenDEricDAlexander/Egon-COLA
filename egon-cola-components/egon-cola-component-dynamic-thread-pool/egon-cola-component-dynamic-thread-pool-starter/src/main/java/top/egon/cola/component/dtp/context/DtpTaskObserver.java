package top.egon.cola.component.dtp.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.thread.TraceRouteCallable;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

/**
 * @description 任务上下文和真实终态观察；核心路径不依赖 Micrometer 或远程存储
 */
@Slf4j
public final class DtpTaskObserver {

    private static final ThreadLocal<Observation> ACTIVE = new ThreadLocal<>();

    @Getter
    private final String executorName;
    private final String appName;
    private final String instanceId;
    private final boolean enabled;
    private final long slowNanos;
    private final long waitNanos;
    private final long logCooldownNanos;
    private final AtomicLong lastLog = new AtomicLong(Long.MIN_VALUE);
    private final LongAdder submitted = new LongAdder();
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder cancelled = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final AtomicBoolean complete;
    private final CopyOnWriteArrayList<BiConsumer<String, Long>> timingRecorders = new CopyOnWriteArrayList<>();
    private final ThreadLocal<Observation> submitting = new ThreadLocal<>();

    public DtpTaskObserver(String appName, String instanceId, String executorName,
                           DynamicThreadPoolAutoProperties.Observation properties, boolean complete) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.enabled = properties.isEnabled();
        this.slowNanos = properties.getSlowTaskThreshold().toNanos();
        this.waitNanos = properties.getQueueWaitThreshold().toNanos();
        this.logCooldownNanos = properties.getAlarm().getCooldown().toNanos();
        this.complete = new AtomicBoolean(complete);
    }

    public long submittedTasks() { return submitted.sum(); }
    public long succeededTasks() { return succeeded.sum(); }
    public long failedTasks() { return failed.sum(); }
    public long cancelledTasks() { return cancelled.sum(); }
    public long rejectedTasks() { return rejected.sum(); }
    public boolean isComplete() { return enabled && complete.get(); }

    public Runnable decorate(Runnable task, boolean createRoot) {
        Objects.requireNonNull(task, "task");
        if (task instanceof ObservedTask observed && observed.observation().owner == this) {
            return task;
        }
        return new ObservedRunnable(newObservation(task, createRoot), task);
    }

    public <T> DtpObservedFutureTask<T> future(Callable<T> task, boolean createRoot) {
        return new DtpObservedFutureTask<>(newObservation(task, createRoot), Objects.requireNonNull(task, "task"));
    }

    public <T> DtpObservedFutureTask<T> future(Runnable task, T result, boolean createRoot) {
        Objects.requireNonNull(task, "task");
        return new DtpObservedFutureTask<>(newObservation(task, createRoot), () -> {
            task.run();
            return result;
        });
    }

    /** 保留拒绝处理器的调用者线程语义，包括 Spring TaskDecorator 包装的任务。 */
    public void dispatch(Runnable task, java.util.concurrent.Executor executor) {
        Observation previous = submitting.get();
        Observation current = task instanceof ObservedTask observed ? observed.observation() : null;
        submitting.set(current);
        try {
            executor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            if (current != null) {
                current.reject();
            }
            throw e;
        } finally {
            if (previous == null) {
                submitting.remove();
            } else {
                submitting.set(previous);
            }
        }
    }

    public void rejected(Runnable task) {
        if (task instanceof ObservedTask observed) { observed.observation().reject(); }
    }

    public void rejectedSubmission() {
        Observation observation = submitting.get();
        if (observation != null) {
            observation.reject();
        }
    }

    public AutoCloseable recordTimings(BiConsumer<String, Long> recorder) {
        timingRecorders.add(Objects.requireNonNull(recorder, "recorder"));
        return () -> timingRecorders.remove(recorder);
    }

    public <T> T invokeAny(Executor executor, Collection<? extends Callable<T>> tasks,
                            boolean timed, long timeoutNanos, boolean createRoot)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(tasks, "tasks");
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks must not be empty");
        }
        List<DtpObservedFutureTask<T>> futures = new ArrayList<>(tasks.size());
        BlockingQueue<DtpObservedFutureTask<T>> completed = new LinkedBlockingQueue<>();
        Iterator<? extends Callable<T>> iterator = tasks.iterator();
        long started = System.nanoTime();
        int active = 0;
        ExecutionException failure = null;
        try {
            while (iterator.hasNext() || active > 0) {
                DtpObservedFutureTask<T> done = completed.poll();
                if (done == null && iterator.hasNext()) {
                    DtpObservedFutureTask<T> next = future(iterator.next(), createRoot);
                    next.onCompletion(completed::add);
                    futures.add(next);
                    executor.execute(next);
                    active++;
                    continue;
                }
                if (done == null) {
                    if (timed) {
                        long remaining = timeoutNanos - (System.nanoTime() - started);
                        done = completed.poll(Math.max(0, remaining), TimeUnit.NANOSECONDS);
                        if (done == null) {
                            throw new TimeoutException();
                        }
                    } else {
                        done = completed.take();
                    }
                }
                active--;
                try {
                    return done.get();
                } catch (ExecutionException e) {
                    failure = e;
                } catch (CancellationException e) {
                    failure = new ExecutionException(e);
                }
            }
            throw failure == null ? new ExecutionException(null) : failure;
        } finally {
            futures.forEach(task -> task.cancel(true));
        }
    }

    static void effectiveContext() {
        Observation observation = ACTIVE.get();
        if (observation != null && observation.discoverContext && observation.contextDiscovered.compareAndSet(false, true)) {
            observation.effective = TraceContext.capture();
        }
    }

    static void wrappedOutcome(Throwable failure) {
        Observation observation = ACTIVE.get();
        if (observation != null && observation.opaque && failure != null) {
            observation.owner.diagnostic(observation, "wrapped-failure", 0, 0, failure);
        }
    }

    private Observation newObservation(Object task, boolean createRoot) {
        Objects.requireNonNull(task, "task");
        TraceContext context;
        if (task instanceof DtpRunnable runnable) {
            context = runnable.context();
        } else if (task instanceof DtpCallable<?> callable) {
            context = callable.context();
        } else {
            context = TraceContext.capture();
            if (createRoot && !(task instanceof TraceRouteRunnable) && !(task instanceof TraceRouteCallable<?>)
                    && (context.mdcContext().get(TraceContext.TRACE_ID) == null
                    || context.mdcContext().get(TraceContext.TRACE_ID).isBlank())) {
                context = TraceContext.root();
            }
        }
        if (enabled) {
            submitted.increment();
        }
        return new Observation(this, context, task instanceof CompletableFuture.AsynchronousCompletionTask,
                task instanceof Future<?> || task instanceof CompletableFuture.AsynchronousCompletionTask);
    }

    private void timing(String kind, long nanos) {
        if (!enabled) {
            return;
        }
        for (BiConsumer<String, Long> recorder : timingRecorders) {
            try {
                recorder.accept(kind, nanos);
            } catch (RuntimeException e) {
                if (allowLog()) {
                    log.warn("动态线程池监控回调失败 app={} instance={} executor={} cause={}",
                            appName, instanceId, executorName, e.getClass().getSimpleName());
                }
            }
        }
    }

    private boolean allowLog() {
        long now = System.nanoTime();
        long previous = lastLog.get();
        return (previous == Long.MIN_VALUE || now - previous >= logCooldownNanos)
                && lastLog.compareAndSet(previous, now);
    }

    private void diagnostic(Observation observation, String result, long wait, long execution, Throwable failure) {
        if (enabled && allowLog()) {
            try (TraceContext.Scope ignored = observation.effective.open()) {
                log.warn("动态线程池任务诊断 app={} instance={} executor={} traceId={} requestId={} result={} waitNanos={} executionNanos={} cause={}",
                        appName, instanceId, executorName, observation.effective.mdcContext().get(TraceContext.TRACE_ID),
                        observation.effective.requestId(), result, wait, execution,
                        failure == null ? "" : failure.getClass().getSimpleName());
            }
        }
    }

    interface ObservedTask {
        Observation observation();
    }

    static final class Observation {
        private final DtpTaskObserver owner;
        private final TraceContext context;
        private final boolean opaque;
        private final boolean discoverContext;
        private final AtomicBoolean contextDiscovered = new AtomicBoolean();
        private final long submittedNanos = System.nanoTime();
        private final AtomicReference<Future.State> outcome = new AtomicReference<>();
        private final AtomicBoolean rejectedOnce = new AtomicBoolean();
        private volatile TraceContext effective;
        boolean managedFuture;

        private Observation(DtpTaskObserver owner, TraceContext context, boolean opaque, boolean discoverContext) {
            this.owner = owner;
            this.context = context;
            this.effective = context;
            this.opaque = opaque;
            this.discoverContext = discoverContext;
        }

        <T> T call(Callable<T> callable) throws Exception {
            long started = System.nanoTime();
            long wait = started - submittedNanos;
            Observation previous = ACTIVE.get();
            try (TraceContext.Scope ignored = context.open()) {
                ACTIVE.set(this);
                owner.timing("wait", wait);
                if (wait >= owner.waitNanos) {
                    owner.diagnostic(this, "slow-wait", wait, 0, null);
                }
                try {
                    T value = callable.call();
                    if (!opaque && !managedFuture) {
                        finish(Future.State.SUCCESS, null);
                    } else if (opaque && outcome.get() == null) {
                        // CompletableFuture 的内部 Runnable 不公开 stage 结果，不能冒称业务成功。
                        owner.complete.set(false);
                    }
                    return value;
                } catch (Exception | Error e) {
                    if (!managedFuture) { finish(Future.State.FAILED, e); }
                    throw e;
                } finally {
                    long duration = System.nanoTime() - started;
                    owner.timing("execution", duration);
                    if (duration >= owner.slowNanos) {
                        owner.diagnostic(this, "slow-execution", wait, duration, null);
                    }
                }
            } finally {
                if (previous == null) {
                    ACTIVE.remove();
                } else {
                    ACTIVE.set(previous);
                }
            }
        }

        void succeed() {
            if (opaque) { owner.complete.set(false); }
            else { finish(Future.State.SUCCESS, null); }
        }
        void fail(Throwable failure) { finish(Future.State.FAILED, failure); }

        void cancel() {
            finish(Future.State.CANCELLED, null);
        }

        void finish(Future.State result, Throwable failure) {
            if (outcome.compareAndSet(null, result) && owner.enabled) {
                switch (result) {
                    case SUCCESS -> owner.succeeded.increment();
                    case FAILED -> {
                        owner.failed.increment();
                        owner.diagnostic(this, "failed", 0, 0, failure);
                    }
                    case CANCELLED -> owner.cancelled.increment();
                    case RUNNING -> throw new IllegalArgumentException("running is not a terminal state");
                }
            }
        }

        void reject() {
            if (rejectedOnce.compareAndSet(false, true) && owner.enabled) {
                owner.rejected.increment();
                owner.diagnostic(this, "rejected", 0, 0, null);
            }
        }
    }

    private static final class ObservedRunnable implements Runnable, ObservedTask {
        private final Observation observation;
        private final Runnable delegate;

        private ObservedRunnable(Observation observation, Runnable delegate) {
            this.observation = observation;
            this.delegate = delegate;
        }

        @Override
        public Observation observation() { return observation; }

        @Override
        public void run() {
            try {
                observation.call(() -> {
                    delegate.run();
                    if (delegate instanceof Future<?> future && future.isDone()) {
                        try {
                            future.get();
                        } catch (CancellationException e) {
                            observation.cancel();
                        } catch (ExecutionException e) {
                            observation.finish(Future.State.FAILED, e.getCause());
                        }
                    }
                    return null;
                });
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while observing completed task", e);
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected checked task exception", e);
            }
        }
    }
}
