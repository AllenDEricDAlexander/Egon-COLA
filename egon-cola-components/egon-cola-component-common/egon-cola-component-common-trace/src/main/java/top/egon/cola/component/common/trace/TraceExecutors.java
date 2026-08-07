package top.egon.cola.component.common.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Factory for executor decorators that propagate the submitting trace context.
 */
public final class TraceExecutors {

    private TraceExecutors() {
    }

    /**
     * Decorates an executor service with per-submission trace propagation.
     *
     * @param executor executor service to decorate
     * @return context-aware executor service
     */
    public static ExecutorService contextAware(ExecutorService executor) {
        Objects.requireNonNull(executor, "executor");
        if (executor instanceof ContextAwareExecutorService) {
            return executor;
        }
        return new ContextAwareExecutorService(executor);
    }

    private static final class ContextAwareExecutorService
            extends AbstractExecutorService {

        private final ExecutorService delegate;

        private ContextAwareExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(TraceSnapshot.capture().wrap(command));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(TraceSnapshot.capture().wrap(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(TraceSnapshot.capture().wrap(task), result);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(TraceSnapshot.capture().wrap(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return delegate.invokeAll(wrapCallables(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks,
                long timeout,
                TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(wrapCallables(tasks));
        }

        @Override
        public <T> T invokeAny(
                Collection<? extends Callable<T>> tasks,
                long timeout,
                TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
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
        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        private <T> Collection<Callable<T>> wrapCallables(
                Collection<? extends Callable<T>> tasks) {
            Objects.requireNonNull(tasks, "tasks");
            TraceSnapshot snapshot = TraceSnapshot.capture();
            List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                wrapped.add(snapshot.wrap(task));
            }
            return wrapped;
        }
    }
}
