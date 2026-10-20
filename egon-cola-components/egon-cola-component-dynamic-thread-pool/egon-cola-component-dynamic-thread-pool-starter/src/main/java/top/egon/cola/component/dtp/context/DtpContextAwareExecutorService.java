package top.egon.cola.component.dtp.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知执行器
 * @Date        下午9:27 2026/6/29
 **/
public class DtpContextAwareExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;

    private final DtpTaskObserver observer;

    public DtpContextAwareExecutorService(ExecutorService delegate) {
        this(delegate, null);
    }

    public DtpContextAwareExecutorService(ExecutorService delegate, DtpTaskObserver observer) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.observer = observer;
    }

    public DtpTaskObserver taskObserver() {
        return observer;
    }

    @Override
    public void execute(Runnable command) {
        if (observer == null) {
            delegate.execute(DtpRunnable.wrap(command));
        } else {
            observer.dispatch(observer.decorate(command, false), delegate);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (observer != null) {
            return super.submit(task);
        }
        return delegate.submit(DtpRunnable.wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (observer != null) {
            return super.submit(task, result);
        }
        return delegate.submit(DtpRunnable.wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (observer != null) {
            return super.submit(task);
        }
        return delegate.submit(DtpCallable.wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (observer != null) {
            return super.invokeAll(tasks);
        }
        return delegate.invokeAll(wrapCallables(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (observer != null) {
            return super.invokeAll(tasks, timeout, unit);
        }
        return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        if (observer != null) {
            try {
                return observer.invokeAny(this, tasks, false, 0, false);
            } catch (TimeoutException e) {
                throw new AssertionError(e);
            }
        }
        return delegate.invokeAny(wrapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (observer != null) {
            return observer.invokeAny(this, tasks, true, unit.toNanos(timeout), false);
        }
        return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return observer == null ? super.newTaskFor(runnable, value) : observer.future(runnable, value, false);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return observer == null ? super.newTaskFor(callable) : observer.future(callable, false);
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

    private <T> Collection<Callable<T>> wrapCallables(Collection<? extends Callable<T>> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrapped.add(DtpCallable.wrap(Objects.requireNonNull(task, "task")));
        }
        return wrapped;
    }
}
