package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class VirtualThreadTimeLimiter implements TimeLimiter, AutoCloseable {

    private final ExecutorService executor;

    public VirtualThreadTimeLimiter() {
        this(Executors.newVirtualThreadPerTaskExecutor());
    }

    VirtualThreadTimeLimiter(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
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
            Throwable cause = exception.getCause();
            throw cause instanceof GuardOperationException operationException
                    ? operationException.getCause()
                    : cause;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static void validate(ExecutionConfig.TimeLimitConfig config) {
        Objects.requireNonNull(config, "config");
        if (config.mode() != TimeLimitMode.ENFORCE || config.executor() != TimeLimiterType.VIRTUAL_THREAD) {
            throw new IllegalArgumentException("VIRTUAL_THREAD is valid only for ENFORCE");
        }
    }

    private static final class GuardOperationException extends RuntimeException {

        private GuardOperationException(Throwable cause) {
            super(cause);
        }
    }
}
