package top.egon.cola.component.accessguard.circuitbreaker;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadPoolTimeoutCircuitBreakerExecutor implements TimeoutCircuitBreakerExecutor {

    private final TimeoutTaskExecutorProvider executorProvider;

    private final RejectResponseInvoker rejectResponseInvoker;

    public ThreadPoolTimeoutCircuitBreakerExecutor(ExecutorService executorService, RejectResponseInvoker rejectResponseInvoker) {
        this(rule -> executorService, rejectResponseInvoker);
    }

    public ThreadPoolTimeoutCircuitBreakerExecutor(TimeoutTaskExecutorProvider executorProvider, RejectResponseInvoker rejectResponseInvoker) {
        this.executorProvider = executorProvider;
        this.rejectResponseInvoker = rejectResponseInvoker;
    }

    @Override
    public Object execute(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context) throws Throwable {
        if (!rule.timeoutEnabled()) {
            return joinPoint.proceed();
        }

        Future<Object> future;
        try {
            future = executorProvider.getExecutor(rule).submit(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    throw new TimeoutCircuitBreakerException("Business invocation failed", throwable);
                }
            });
        } catch (RejectedExecutionException e) {
            return rejectResponseInvoker.reject(joinPoint, rule, context, joinPoint.getArgs());
        }

        try {
            return future.get(rule.timeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            if (rule.cancelRunningTask()) {
                future.cancel(true);
            }
            return rejectResponseInvoker.reject(joinPoint, rule, context, joinPoint.getArgs());
        } catch (ExecutionException e) {
            Throwable businessError = unwrap(e);
            if (rule.fallbackOnException()) {
                return rejectResponseInvoker.reject(joinPoint, rule, context, joinPoint.getArgs());
            }
            throw businessError;
        }
    }

    public void shutdown() {
        ExecutorService executorService = executorProvider.getExecutor(null);
        executorService.shutdownNow();
    }

    private Throwable unwrap(ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        if (cause instanceof TimeoutCircuitBreakerException && cause.getCause() != null) {
            return cause.getCause();
        }
        return cause;
    }
}
