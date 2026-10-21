package top.egon.cola.component.dtp.executor.virtual;

import top.egon.cola.component.dtp.context.DtpContextAwareExecutorService;
import top.egon.cola.component.dtp.context.DtpTaskObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author      有罗敷的马同学
 * @description 有界虚拟线程执行器
 * @Date        下午9:52 2026/6/29
 **/
public class BoundedVirtualThreadExecutor extends AbstractExecutorService {

    private final String threadNamePrefix;

    private volatile DtpTaskObserver taskObserver;

    public synchronized void setTaskObserver(DtpTaskObserver observer) {
        if (taskObserver != null && taskObserver != observer) {
            throw new IllegalStateException("virtual executor already has an observer");
        }
        taskObserver = Objects.requireNonNull(observer, "observer");
    }

    private final AdjustableSemaphore semaphore;

    private final ExecutorService delegate;

    private final AtomicInteger concurrencyLimit;

    private final AtomicLong runningTasks = new AtomicLong();

    private final AtomicLong submittedTasks = new AtomicLong();

    private final AtomicLong completedTasks = new AtomicLong();

    private final AtomicLong failedTasks = new AtomicLong();

    private final AtomicLong rejectedTasks = new AtomicLong();

    public BoundedVirtualThreadExecutor(String threadNamePrefix, int concurrencyLimit) {
        if (concurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        this.threadNamePrefix = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");
        this.concurrencyLimit = new AtomicInteger(concurrencyLimit);
        this.semaphore = new AdjustableSemaphore(concurrencyLimit);
        java.util.concurrent.ThreadFactory factory = Thread.ofVirtual().name(threadNamePrefix + "-", 0).factory();
        ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(task -> {
            DtpTaskObserver observer = taskObserver;
            return (observer == null ? factory : observer.threadFactory(factory)).newThread(task);
        });
        this.delegate = new DtpContextAwareExecutorService(virtualExecutor);
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        if (command instanceof MetricsAwareTask) {
            schedule(command, false);
            return;
        }
        schedule(command, true);
    }

    @Override
    public Future<?> submit(Runnable task) {
        RunnableFuture<Void> future = newTaskFor(task, null);
        schedule(future, false);
        return future;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        RunnableFuture<T> future = newTaskFor(task, result);
        schedule(future, false);
        return future;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        RunnableFuture<T> future = newTaskFor(task);
        schedule(future, false);
        return future;
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        Runnable task = Objects.requireNonNull(runnable, "runnable");
        return new MetricsFutureTask<>(this, task, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        Callable<T> task = Objects.requireNonNull(callable, "callable");
        return new MetricsFutureTask<>(this, task);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0L);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(unit, "unit");
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
            throws InterruptedException, ExecutionException, TimeoutException {
        Objects.requireNonNull(tasks, "tasks");
        int taskCount = tasks.size();
        if (taskCount == 0) {
            throw new IllegalArgumentException();
        }
        List<Future<T>> futures = new ArrayList<>(taskCount);
        BlockingQueue<Future<T>> completionQueue = new LinkedBlockingQueue<>();
        Iterator<? extends Callable<T>> iterator = tasks.iterator();
        ExecutionException executionException = null;
        long deadline = timed ? System.nanoTime() + nanos : 0L;
        try {
            futures.add(submitForCompletion(iterator.next(), completionQueue));
            taskCount--;
            int activeTasks = 1;
            for (; ; ) {
                Future<T> future = completionQueue.poll();
                if (future == null) {
                    if (taskCount > 0) {
                        taskCount--;
                        futures.add(submitForCompletion(iterator.next(), completionQueue));
                        activeTasks++;
                    } else if (activeTasks == 0) {
                        break;
                    } else if (timed) {
                        future = completionQueue.poll(nanos, TimeUnit.NANOSECONDS);
                        if (future == null) {
                            throw new TimeoutException();
                        }
                        nanos = deadline - System.nanoTime();
                    } else {
                        future = completionQueue.take();
                    }
                }
                if (future != null) {
                    activeTasks--;
                    try {
                        return future.get();
                    } catch (ExecutionException e) {
                        executionException = e;
                    } catch (CancellationException e) {
                        executionException = new ExecutionException(e);
                    }
                }
            }
            if (executionException == null) {
                executionException = new ExecutionException(null);
            }
            throw executionException;
        } finally {
            for (Future<T> future : futures) {
                future.cancel(true);
            }
        }
    }

    private <T> Future<T> submitForCompletion(Callable<T> task, BlockingQueue<Future<T>> completionQueue) {
        QueueingMetricsFutureTask<T> future = new QueueingMetricsFutureTask<>(
                Objects.requireNonNull(task, "task"),
                completionQueue
        );
        schedule(future, false);
        return future;
    }

    private void schedule(Runnable command, boolean countOutcome) {
        DtpTaskObserver observer = taskObserver;
        Runnable observed;
        if (observer != null && command instanceof MetricsFutureTask<?> future) {
            future.lifecycleCallable.externallyScheduled = true;
            observed = observer.decorateFuture(future.originalTask, future, future::failBeforeExecution, false);
        } else {
            observed = observer == null ? command : observer.decorate(command, false);
        }
        submittedTasks.incrementAndGet();
        if (!semaphore.tryAcquire()) {
            rejectedTasks.incrementAndGet();
            if (observer != null) { observer.rejected(observed); }
            throw new RejectedExecutionException("Virtual executor concurrency limit exceeded");
        }
        boolean accepted = false;
        try {
            if (command instanceof MetricsFutureTask<?> && observer == null) {
                delegate.execute(observed);
            } else {
                delegate.execute(() -> {
                    runningTasks.incrementAndGet();
                    try {
                        if (countOutcome) {
                            runAndCount(observed);
                        } else {
                            observed.run();
                        }
                    } finally {
                        runningTasks.decrementAndGet();
                        semaphore.release();
                    }
                });
            }
            accepted = true;
        } catch (RejectedExecutionException e) {
            if (observer != null) { observer.rejected(observed); }
            rejectedTasks.incrementAndGet();
            throw e;
        } finally {
            if (!accepted) {
                semaphore.release();
            }
        }
    }

    private void runAndCount(Runnable command) {
        try {
            command.run();
            completedTasks.incrementAndGet();
        } catch (Throwable e) {
            failedTasks.incrementAndGet();
            throw e;
        }
    }

    private <T> Callable<T> callAndCount(Callable<T> callable) {
        return () -> {
            try {
                T result = callable.call();
                completedTasks.incrementAndGet();
                return result;
            } catch (Throwable e) {
                failedTasks.incrementAndGet();
                throw e;
            }
        };
    }

    public int concurrencyLimit() {
        return concurrencyLimit.get();
    }

    public long runningTasks() {
        return runningTasks.get();
    }

    public long submittedTasks() {
        return submittedTasks.get();
    }

    public long completedTasks() {
        return completedTasks.get();
    }

    public long failedTasks() {
        return failedTasks.get();
    }

    public long rejectedTasks() {
        return rejectedTasks.get();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public String threadNamePrefix() {
        return threadNamePrefix;
    }

    public synchronized void updateConcurrencyLimit(int concurrencyLimit) {
        if (concurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        int previous = this.concurrencyLimit.getAndSet(concurrencyLimit);
        int delta = concurrencyLimit - previous;
        if (delta > 0) {
            semaphore.release(delta);
        } else if (delta < 0) {
            semaphore.reduce(-delta);
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    private interface MetricsAwareTask {
    }

    private static class MetricsFutureTask<T> extends FutureTask<T> implements MetricsAwareTask {

        private final MetricsLifecycleCallable<T> lifecycleCallable;
        private final Object originalTask;

        private MetricsFutureTask(BoundedVirtualThreadExecutor owner, Runnable runnable, T result) {
            this(new MetricsLifecycleCallable<>(owner, () -> {
                runnable.run();
                return result;
            }), runnable);
        }

        private MetricsFutureTask(BoundedVirtualThreadExecutor owner, Callable<T> callable) {
            this(new MetricsLifecycleCallable<>(owner, callable), callable);
        }

        private MetricsFutureTask(MetricsLifecycleCallable<T> lifecycleCallable, Object originalTask) {
            super(lifecycleCallable);
            this.lifecycleCallable = lifecycleCallable;
            this.originalTask = originalTask;
        }

        private void failBeforeExecution(Throwable failure) {
            setException(failure);
        }

        @Override
        public void run() {
            try {
                super.run();
            } finally {
                if (!lifecycleCallable.externallyScheduled && !lifecycleCallable.runningStarted()) {
                    lifecycleCallable.releasePermitOnce();
                }
            }
        }

    }

    private static final class MetricsLifecycleCallable<T> implements Callable<T> {

        private final BoundedVirtualThreadExecutor owner;

        private final Callable<T> callable;

        private final AtomicBoolean runningStarted = new AtomicBoolean();

        private final AtomicBoolean permitReleased = new AtomicBoolean();
        private volatile boolean externallyScheduled;

        private MetricsLifecycleCallable(BoundedVirtualThreadExecutor owner, Callable<T> callable) {
            this.owner = owner;
            this.callable = owner.callAndCount(callable);
        }

        @Override
        public T call() throws Exception {
            runningStarted.set(true);
            if (externallyScheduled) { return callable.call(); }
            owner.runningTasks.incrementAndGet();
            try {
                return callable.call();
            } finally {
                owner.runningTasks.decrementAndGet();
                releasePermitOnce();
            }
        }

        private boolean runningStarted() {
            return runningStarted.get();
        }

        private void releasePermitOnce() {
            if (permitReleased.compareAndSet(false, true)) {
                owner.semaphore.release();
            }
        }

    }

    private final class QueueingMetricsFutureTask<T> extends MetricsFutureTask<T> {

        private final BlockingQueue<Future<T>> completionQueue;

        private QueueingMetricsFutureTask(Callable<T> callable, BlockingQueue<Future<T>> completionQueue) {
            super(BoundedVirtualThreadExecutor.this, callable);
            this.completionQueue = completionQueue;
        }

        @Override
        protected void done() {
            completionQueue.add(this);
        }

    }

    private static final class AdjustableSemaphore extends Semaphore {

        private AdjustableSemaphore(int permits) {
            super(permits);
        }

        private void reduce(int reduction) {
            reducePermits(reduction);
        }

    }

}
