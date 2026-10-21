package top.egon.cola.component.dtp.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.enums.ExceptionCategory;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.thread.TraceRouteCallable;
import top.egon.cola.component.common.trace.thread.TraceRouteRunnable;
import top.egon.cola.component.common.trace.thread.TraceRouteSupplier;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.exception.DtpContextPropagationException;
import top.egon.cola.component.dtp.exception.DtpTaskExceptionHandler;
import top.egon.cola.component.dtp.exception.DtpTaskFailureBO;
import top.egon.cola.component.dtp.exception.LoggingDtpTaskExceptionHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.Consumer;

/** 单任务的上下文、真实终态和失败通知；不依赖远程存储或指标后端。 */
@Slf4j
public final class DtpTaskObserver {
    private static final ThreadLocal<Observation> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<DtpTaskObserver> WORKER = new ThreadLocal<>();
    public static final List<String> CONTEXT_PHASES = List.of("CAPTURE", "BACKUP", "INSTALL", "RESTORE");

    @Getter
    private final String executorName;
    private final String appName;
    private final String instanceId;
    private final boolean enabled;
    private final long slowNanos;
    private final long waitNanos;
    private final long logCooldownNanos;
    private final Supplier<DtpTaskContextManager> contexts;
    private final Supplier<DtpTaskExceptionHandler> handlers;
    private final Supplier<ValidationUtils> validation;
    private final DynamicThreadPoolAutoProperties.ExceptionHandling exceptionHandling;
    private final AtomicLong lastLog = new AtomicLong(Long.MIN_VALUE);
    private final LogBudget systemErrors = new LogBudget();
    private final LogBudget businessErrors = new LogBudget();
    private final LongAdder submitted = new LongAdder();
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder cancelled = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final LongAdder reporterFailures = new LongAdder();
    private final LongAdder uncaught = new LongAdder();
    private final Map<String, LongAdder> contextFailures = Map.of(
            "CAPTURE", new LongAdder(), "BACKUP", new LongAdder(), "INSTALL", new LongAdder(), "RESTORE", new LongAdder());
    private final AtomicBoolean complete;
    private final CopyOnWriteArrayList<BiConsumer<String, Long>> timingRecorders = new CopyOnWriteArrayList<>();
    private final ThreadLocal<Observation> submitting = new ThreadLocal<>();
    private final ThreadLocal<EscapedFailure> escaped = new ThreadLocal<>();
    private volatile ExecutorKind executorKind;

    public DtpTaskObserver(String appName, String instanceId, String executorName,
                           DynamicThreadPoolAutoProperties.Observation properties, boolean complete) {
        this(appName, instanceId, executorName, properties, complete, new DtpTaskContextManager());
    }

    public DtpTaskObserver(String appName, String instanceId, String executorName,
                           DynamicThreadPoolAutoProperties.Observation properties, boolean complete,
                           DtpTaskContextManager contextManager) {
        this(appName, instanceId, executorName, properties, complete, () -> contextManager,
                LoggingDtpTaskExceptionHandler::new, () -> null, new DynamicThreadPoolAutoProperties.ExceptionHandling());
    }

    public DtpTaskObserver(String appName, String instanceId, String executorName,
                           DynamicThreadPoolAutoProperties.Observation properties, boolean complete,
                           Supplier<DtpTaskContextManager> contexts, Supplier<DtpTaskExceptionHandler> handlers,
                           Supplier<ValidationUtils> validation,
                           DynamicThreadPoolAutoProperties.ExceptionHandling exceptionHandling) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.enabled = properties.isEnabled();
        this.slowNanos = properties.getSlowTaskThreshold().toNanos();
        this.waitNanos = properties.getQueueWaitThreshold().toNanos();
        this.logCooldownNanos = properties.getAlarm().getCooldown().toNanos();
        this.complete = new AtomicBoolean(complete);
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.exceptionHandling = Objects.requireNonNull(exceptionHandling, "exceptionHandling");
    }

    public long submittedTasks() { return submitted.sum(); }
    public long succeededTasks() { return succeeded.sum(); }
    public long failedTasks() { return failed.sum(); }
    public long cancelledTasks() { return cancelled.sum(); }
    public long rejectedTasks() { return rejected.sum(); }
    public long reporterFailures() { return reporterFailures.sum(); }
    public long uncaughtFailures() { return uncaught.sum(); }
    public long contextFailures(String phase) { return contextFailures.get(phase).sum(); }
    public boolean isComplete() { return enabled && complete.get(); }
    public void setExecutorKind(ExecutorKind kind) { executorKind = kind; }

    public Runnable decorate(Runnable task, boolean createRoot) {
        Objects.requireNonNull(task, "task");
        if (task instanceof ObservedTask observed && observed.observation().owner == this) {
            return task;
        }
        Observation observation = newObservation(task, createRoot, null);
        if (task instanceof Future<?> && !observation.opaque) {
            observation.managedFuture = true;
            observation.source = "SUBMIT";
        }
        return new ObservedRunnable(observation, task);
    }

    /** 组件拥有的Future提供合法异常完成回调，安装失败仍依其真实终态报告。 */
    public Runnable decorateFuture(Object originalTask, java.util.concurrent.RunnableFuture<?> future,
                                   Consumer<Throwable> failureCompletion, boolean createRoot) {
        Observation observation = newObservation(originalTask, createRoot, null);
        observation.managedFuture = true;
        observation.source = "SUBMIT";
        return new ObservedRunnable(observation, future, failureCompletion);
    }

    public <T> DtpObservedFutureTask<T> future(Callable<T> task, boolean createRoot) {
        Observation observation = newObservation(Objects.requireNonNull(task, "task"), createRoot, null);
        observation.source = "SUBMIT";
        return new DtpObservedFutureTask<>(observation, task);
    }

    public <T> DtpObservedFutureTask<T> future(Runnable task, T result, boolean createRoot) {
        Observation observation = newObservation(Objects.requireNonNull(task, "task"), createRoot, null);
        observation.source = "SUBMIT";
        return new DtpObservedFutureTask<>(observation, () -> { task.run(); return result; });
    }

    /** 在调度之前持有真正的root，安装失败也能结束Future，不留下永久等待的阶段。 */
    public <T> CompletableFuture<T> completable(Executor executor, Object originalTask,
                                               Callable<T> action, boolean createRoot) {
        Observation observation = newObservation(Objects.requireNonNull(originalTask, "task"), createRoot, null);
        observation.source = "COMPLETABLE_FUTURE";
        observation.managedFuture = true;
        CompletableFuture<T> root = new CompletableFuture<>();
        root.whenComplete((value, failure) -> {
            if (root.isCancelled()) { observation.cancel(); }
            else if (failure != null) { observation.fail(failure); }
            else { observation.succeed(); }
        });
        dispatch(new RootTask<>(observation, root, action), executor);
        return root;
    }

    public Executor contextualExecutor(Executor executor, boolean createRoot) {
        DtpTaskContextManager manager = contexts.get();
        DtpTaskContextBO captured = capture(manager, null, createRoot);
        return task -> {
            Objects.requireNonNull(task, "task");
            Observation observation = newObservation(task, createRoot, captured);
            dispatch(new ObservedRunnable(observation, task), executor);
        };
    }

    /** 保留拒绝处理器的调用者线程语义，包括Spring装饰后的任务。 */
    public void dispatch(Runnable task, Executor executor) {
        Observation previous = submitting.get();
        Observation current = task instanceof ObservedTask observed ? observed.observation() : null;
        submitting.set(current);
        try { executor.execute(task); }
        catch (java.util.concurrent.RejectedExecutionException e) {
            if (current != null) { current.reject(); }
            throw e;
        } finally {
            if (previous == null) { submitting.remove(); }
            else { submitting.set(previous); }
        }
    }

    public void rejected(Runnable task) {
        if (task instanceof ObservedTask observed) { observed.observation().reject(); }
    }

    public void rejectedSubmission() {
        Observation observation = submitting.get();
        if (observation != null) { observation.reject(); }
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

    public static boolean asyncFailure(Throwable failure, String methodName) {
        Observation observation = ACTIVE.get();
        if (observation == null) { return false; }
        observation.source = "ASYNC_VOID";
        observation.methodName = methodName;
        observation.fail(failure);
        return true;
    }

    private Observation newObservation(Object task, boolean createRoot, DtpTaskContextBO fixed) {
        Objects.requireNonNull(task, "task");
        DtpTaskContextManager manager = contexts.get();
        DtpTaskContextBO snapshot = fixed == null ? capture(manager, task, createRoot) : fixed;
        TraceContext explicit = explicitContext(task);
        if (fixed != null && explicit != null) {
            snapshot = new DtpTaskContextBO(explicit, fixed.getRegistrationIdentity(), fixed.getValues());
        }
        if (enabled) { submitted.increment(); }
        boolean unknownTrace = task instanceof TraceRouteRunnable && !(task instanceof DtpRunnable)
                || task instanceof TraceRouteCallable<?> && !(task instanceof DtpCallable<?>)
                || task instanceof TraceRouteSupplier<?> && !(task instanceof DtpSupplier<?>);
        if (unknownTrace || task instanceof CompletableFuture.AsynchronousCompletionTask) { complete.set(false); }
        return new Observation(this, manager, snapshot, unknownTrace, task instanceof CompletableFuture.AsynchronousCompletionTask,
                task instanceof Future<?> || task instanceof CompletableFuture.AsynchronousCompletionTask
                        || task instanceof TraceRouteSupplier<?>);
    }

    private DtpTaskContextBO capture(DtpTaskContextManager manager, Object task, boolean createRoot) {
        TraceContext trace = explicitContext(task);
        if (trace == null) {
            trace = TraceContext.capture();
            if (createRoot && !(task instanceof TraceRouteRunnable) && !(task instanceof TraceRouteCallable<?>)
                    && !(task instanceof TraceRouteSupplier<?>)
                    && (trace.mdcContext().get(TraceContext.TRACE_ID) == null
                    || trace.mdcContext().get(TraceContext.TRACE_ID).isBlank())) {
                trace = TraceContext.root();
            }
        }
        try { return manager.capture(trace); }
        catch (DtpContextPropagationException failure) {
            contextFailure(failure);
            safeDiagnostic(failure, trace, Map.of("contextPhase", failure.getPhase(),
                    "accessorKey", failure.getAccessorKey(), "executorName", executorName));
            throw failure;
        }
    }

    private static TraceContext explicitContext(Object task) {
        if (task instanceof DtpRunnable runnable) { return runnable.context(); }
        if (task instanceof DtpCallable<?> callable) { return callable.context(); }
        if (task instanceof DtpSupplier<?> supplier) { return supplier.context(); }
        return null;
    }

    private void contextFailure(DtpContextPropagationException failure) {
        LongAdder counter = contextFailures.get(failure.getPhase());
        if (counter != null) { counter.increment(); }
    }

    private void timing(String kind, long nanos) {
        if (!enabled) { return; }
        for (BiConsumer<String, Long> recorder : timingRecorders) {
            try { recorder.accept(kind, nanos); }
            catch (RuntimeException e) {
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
            TraceContext trace = effectiveTrace(observation);
            try (TraceContext.Scope ignored = trace.open()) {
                log.warn("动态线程池任务诊断 app={} instance={} executor={} traceId={} requestId={} result={} waitNanos={} executionNanos={} cause={}",
                        appName, instanceId, executorName, trace.mdcContext().get(TraceContext.TRACE_ID),
                        trace.requestId(), result, wait, execution,
                        failure == null ? "" : failure.getClass().getSimpleName());
            }
        }
    }

    private static TraceContext effectiveTrace(Observation observation) {
        if (!observation.unknownTrace) { return observation.effective; }
        try (TraceContext.Scope ignored = TraceContext.capture().open()) {
            TraceContext.clear();
            return TraceContext.capture();
        }
    }

    private void report(Observation observation, Throwable failure, String source, String methodName) {
        if (!exceptionHandling.isEnabled() || "ASYNC_VOID".equals(source) && !exceptionHandling.isAsyncVoidEnabled()
                || observation != null && !observation.reported.compareAndSet(false, true)) { return; }
        TraceContext trace = observation == null ? TraceContext.capture() : effectiveTrace(observation);
        try (TraceContext.Scope ignored = trace.open()) {
            LogBudget budget = businessFailure(failure) ? businessErrors : systemErrors;
            boolean eligible = budget.allow(exceptionHandling.getLogInterval().toNanos());
            DtpTaskFailureBO value = DtpTaskFailureBO.builder().failureId(UUID.randomUUID().toString())
                    .appName(appName).instanceId(instanceId).executorName(executorName).executorKind(executorKind)
                    .source(source).traceId(trace.mdcContext().get(TraceContext.TRACE_ID)).requestId(trace.requestId())
                    .threadName(Thread.currentThread().getName()).methodName(methodName).observedAt(Instant.now())
                    .cause(failure).logEligible(eligible).suppressedCount(eligible ? budget.suppressed.getAndSet(0) : 0).build();
            ValidationUtils checker = validation.get();
            if (checker != null) { checker.validate(value, DtpTaskFailureBO.Report.class); }
            handlers.get().handle(value);
        } catch (Throwable reporterFailure) {
            if (reporterFailure instanceof VirtualMachineError fatal) { throw fatal; }
            if (reporterFailure instanceof ThreadDeath fatal) { throw fatal; }
            reporterFailures.increment();
            safeDiagnostic(reporterFailure, trace, Map.of("executorName", executorName, "source", "REPORTER"));
        }
    }

    private static boolean businessFailure(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null && seen.add(failure)) {
            failure = failure.getCause();
        }
        return failure instanceof CommonException common && common.getExceptionCategory() == ExceptionCategory.BUSINESS;
    }

    private static void safeDiagnostic(Throwable failure, TraceContext trace, Map<String, String> fields) {
        try (TraceContext.Scope ignored = trace.open()) { ExceptionLogger.log(log, failure, fields); }
        catch (RuntimeException e) {
            try { log.error("动态线程池诊断出口失败 cause={}", e.getClass().getName()); }
            catch (RuntimeException ignored) { /* 不可用的日志后端不能改写原任务协议。 */ }
        }
    }

    /** 只在组件创建的线程上组合兜底，原工厂和原handler均保留。 */
    public ThreadFactory threadFactory(ThreadFactory factory) {
        Objects.requireNonNull(factory, "factory");
        if (!exceptionHandling.isUncaughtEnabled()) { return factory; }
        if (factory instanceof ObservingThreadFactory observed && observed.owner() == this) { return factory; }
        return new ObservingThreadFactory(factory);
    }

    private final class ObservingThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private ObservingThreadFactory(ThreadFactory delegate) { this.delegate = delegate; }
        private DtpTaskObserver owner() { return DtpTaskObserver.this; }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = delegate.newThread(() -> {
                DtpTaskObserver previous = WORKER.get();
                WORKER.set(DtpTaskObserver.this);
                boolean normal = false;
                try { runnable.run(); normal = true; }
                finally {
                    if (previous == null) { WORKER.remove(); } else { WORKER.set(previous); }
                    if (normal) { escaped.remove(); }
                }
            });
            if (thread == null) { return null; }
            Thread.UncaughtExceptionHandler original = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler((worker, failure) -> {
                try {
                    uncaught.increment();
                    EscapedFailure record = escaped.get();
                    if (record != null && record.failure == failure) {
                        if (!record.observation.opaque) {
                            report(record.observation, failure, record.observation.source, record.observation.methodName);
                        }
                    } else {
                        // MDC可能已经恢复；未知worker故障不能借用别的任务的Trace。
                        try (TraceContext.Scope ignored = TraceContext.capture().open()) {
                            TraceContext.clear();
                            report(null, failure, "UNCAUGHT", null);
                        }
                    }
                } finally {
                    escaped.remove();
                    if (original != null) { original.uncaughtException(worker, failure); }
                }
            });
            return thread;
        }
    }

    interface ObservedTask {
        Observation observation();
    }

    static final class Observation {
        private final DtpTaskObserver owner;
        private final DtpTaskContextManager manager;
        private final DtpTaskContextBO context;
        private final boolean unknownTrace;
        private final boolean opaque;
        private final boolean discoverContext;
        private final AtomicBoolean contextDiscovered = new AtomicBoolean();
        private final long submittedNanos = System.nanoTime();
        private final AtomicReference<Future.State> outcome = new AtomicReference<>();
        private final AtomicBoolean rejectedOnce = new AtomicBoolean();
        private final AtomicBoolean reported = new AtomicBoolean();
        private volatile TraceContext effective;
        private String source = "EXECUTE";
        private String methodName;
        boolean managedFuture;

        private Observation(DtpTaskObserver owner, DtpTaskContextManager manager, DtpTaskContextBO context,
                            boolean unknownTrace, boolean opaque, boolean discoverContext) {
            this.owner = owner;
            this.manager = manager;
            this.context = context;
            this.effective = context.getTraceContext();
            this.unknownTrace = unknownTrace;
            this.opaque = opaque;
            this.discoverContext = discoverContext;
        }

        <T> T call(Callable<T> callable) throws Exception {
            long started = System.nanoTime();
            long wait = started - submittedNanos;
            Observation previous = ACTIVE.get();
            boolean normal = false;
            if (WORKER.get() == owner) { owner.escaped.remove(); }
            try {
                try (DtpTaskContextManager.Scope ignored = manager.open(context, owner::contextFailure)) {
                    ACTIVE.set(this);
                    owner.timing("wait", wait);
                    if (wait >= owner.waitNanos) { owner.diagnostic(this, "slow-wait", wait, 0, null); }
                    try {
                        T value = callable.call();
                        if (!opaque && !managedFuture) { finish(Future.State.SUCCESS, null); }
                        else if (opaque && outcome.get() == null) { owner.complete.set(false); }
                        normal = true;
                        return value;
                    } catch (Exception | Error e) {
                        failExecution(e);
                        escape(e);
                        throw e;
                    } finally {
                        long duration = System.nanoTime() - started;
                        owner.timing("execution", duration);
                        if (duration >= owner.slowNanos) { owner.diagnostic(this, "slow-execution", wait, duration, null); }
                    }
                }
            } catch (DtpContextPropagationException e) {
                failExecution(e);
                escape(e);
                throw e;
            } finally {
                if (previous == null) { ACTIVE.remove(); } else { ACTIVE.set(previous); }
                if ((normal || managedFuture) && WORKER.get() == owner) { owner.escaped.remove(); }
            }
        }

        private void escape(Throwable failure) {
            if (!managedFuture && WORKER.get() == owner) { owner.escaped.set(new EscapedFailure(this, failure)); }
        }

        private void failExecution(Throwable failure) {
            if (managedFuture) { return; }
            if (opaque) {
                owner.complete.set(false);
                if (failure instanceof DtpContextPropagationException propagation) {
                    manager.diagnostic(propagation, effectiveTrace(this));
                } else {
                    safeDiagnostic(failure, effectiveTrace(this), Map.of("source", "OPAQUE_EXECUTION"));
                }
            } else { finish(Future.State.FAILED, failure); }
        }

        void succeed() {
            if (opaque) { owner.complete.set(false); }
            else { finish(Future.State.SUCCESS, null); }
        }
        void fail(Throwable failure) { finish(Future.State.FAILED, failure); }
        void cancel() { finish(Future.State.CANCELLED, null); }

        void finish(Future.State result, Throwable failure) {
            if (!outcome.compareAndSet(null, result)) { return; }
            if (owner.enabled) {
                switch (result) {
                    case SUCCESS -> owner.succeeded.increment();
                    case FAILED -> owner.failed.increment();
                    case CANCELLED -> owner.cancelled.increment();
                    case RUNNING -> throw new IllegalArgumentException("running is not a terminal state");
                }
            }
            if (result == Future.State.FAILED) { owner.report(this, failure, source, methodName); }
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
        private final Consumer<Throwable> failureCompletion;
        private ObservedRunnable(Observation observation, Runnable delegate) {
            this(observation, delegate, null);
        }
        private ObservedRunnable(Observation observation, Runnable delegate, Consumer<Throwable> failureCompletion) {
            this.observation = observation;
            this.delegate = delegate;
            this.failureCompletion = failureCompletion;
            if (delegate instanceof Future<?> && !observation.opaque) {
                observation.managedFuture = true;
                observation.source = "SUBMIT";
            }
        }
        @Override
        public Observation observation() { return observation; }

        @Override
        public void run() {
            if (!observation.opaque && delegate instanceof Future<?> future && future.isCancelled()) {
                observation.cancel();
                return;
            }
            try {
                observation.call(() -> {
                    delegate.run();
                    if (!observation.opaque && delegate instanceof Future<?> future) { observeFuture(future); }
                    return null;
                });
            } catch (RuntimeException | Error e) {
                if (!observation.opaque && delegate instanceof Future<?> future) {
                    if (failureCompletion != null) { failureCompletion.accept(e); }
                    else {
                        future.cancel(false);
                        observation.owner.complete.set(false);
                        safeDiagnostic(e, effectiveTrace(observation), Map.of("source", "CONTEXT_FUTURE"));
                    }
                    observeFuture(future);
                    return;
                }
                throw e;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while observing completed task", e);
            } catch (Exception e) { throw new IllegalStateException("Unexpected checked task exception", e); }
        }

        private void observeFuture(Future<?> future) {
            if (!future.isDone()) { observation.owner.complete.set(false); return; }
            try { future.get(); observation.succeed(); }
            catch (CancellationException e) { observation.cancel(); }
            catch (ExecutionException e) { observation.fail(e.getCause()); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                observation.owner.complete.set(false);
            }
        }
    }

    private static final class RootTask<T> implements Runnable, ObservedTask {
        private final Observation observation;
        private final CompletableFuture<T> root;
        private final Callable<T> action;
        private RootTask(Observation observation, CompletableFuture<T> root, Callable<T> action) {
            this.observation = observation;
            this.root = root;
            this.action = action;
        }
        @Override
        public Observation observation() { return observation; }

        @Override
        public void run() {
            if (root.isDone()) { return; }
            try {
                observation.call(() -> {
                    if (!root.isDone()) { root.complete(action.call()); }
                    return null;
                });
            } catch (Throwable failure) {
                // 与JDK CF任务相同：错误保存在原root，包含执行业务前的安装错误。
                root.completeExceptionally(failure);
            }
        }
    }

    private static final class EscapedFailure {
        private final Observation observation;
        private final Throwable failure;
        private EscapedFailure(Observation observation, Throwable failure) {
            this.observation = observation;
            this.failure = failure;
        }
    }

    private static final class LogBudget {
        private final AtomicLong last = new AtomicLong(Long.MIN_VALUE);
        private final AtomicLong suppressed = new AtomicLong();
        private boolean allow(long interval) {
            long now = System.nanoTime();
            long previous = last.get();
            if ((previous == Long.MIN_VALUE || now - previous >= interval) && last.compareAndSet(previous, now)) { return true; }
            suppressed.incrementAndGet();
            return false;
        }
    }
}
