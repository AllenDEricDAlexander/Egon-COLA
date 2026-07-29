package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ThreadPoolTimeLimiter implements TimeLimiter, AutoCloseable {

    private final ThreadPoolExecutor executor;

    public ThreadPoolTimeLimiter(
            String name,
            int corePoolSize,
            int maxPoolSize,
            Duration keepAlive,
            int queueCapacity
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("thread-pool name must not be blank");
        }
        if (corePoolSize <= 0 || maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("thread-pool sizes are invalid");
        }
        if (keepAlive == null || keepAlive.isNegative()) {
            throw new IllegalArgumentException("thread-pool keepAlive must not be negative");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("thread-pool queueCapacity must not be negative");
        }
        BlockingQueue<Runnable> queue = queueCapacity == 0
                ? new SynchronousQueue<>()
                : new ArrayBlockingQueue<>(queueCapacity);
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAlive.toNanos(),
                TimeUnit.NANOSECONDS,
                queue,
                Thread.ofPlatform().name(name.trim() + "-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public Object execute(GuardInvocation invocation, ExecutionConfig.TimeLimitConfig config) throws Throwable {
        Objects.requireNonNull(invocation, "invocation");
        validate(config);
        Future<Object> future;
        try {
            future = executor.submit(() -> {
                try {
                    return invocation.continuation().execute();
                } catch (Throwable throwable) {
                    throw new GuardOperationException(throwable);
                }
            });
        } catch (RejectedExecutionException exception) {
            throw new ExecutorRejectedException(exception);
        }
        try {
            return future.get(config.timeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            if (config.cancelRunningTask()) {
                future.cancel(true);
            }
            throw new TimeLimitExceededException(config.timeout());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException exception) {
            throw unwrap(exception.getCause());
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static void validate(ExecutionConfig.TimeLimitConfig config) {
        Objects.requireNonNull(config, "config");
        if (config.mode() != TimeLimitMode.ENFORCE || config.executor() != TimeLimiterType.THREAD_POOL) {
            throw new IllegalArgumentException("THREAD_POOL is valid only for ENFORCE");
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        return throwable instanceof GuardOperationException operationException
                ? operationException.getCause()
                : throwable;
    }

    private static final class GuardOperationException extends RuntimeException {

        private GuardOperationException(Throwable cause) {
            super(cause);
        }
    }
}
