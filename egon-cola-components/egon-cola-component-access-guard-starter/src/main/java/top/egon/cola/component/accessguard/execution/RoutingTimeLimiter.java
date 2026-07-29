package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RoutingTimeLimiter implements TimeLimiter, AutoCloseable {

    private final Map<TimeLimiterType, TimeLimiter> delegates;
    private final AtomicBoolean closed = new AtomicBoolean();

    public RoutingTimeLimiter(Map<TimeLimiterType, TimeLimiter> delegates) {
        this.delegates = Map.copyOf(Objects.requireNonNull(delegates, "delegates"));
    }

    @Override
    public Object execute(GuardInvocation invocation, ExecutionConfig.TimeLimitConfig config) throws Throwable {
        if (closed.get()) {
            throw new ExecutorRejectedException(new IllegalStateException("TimeLimiter is closed"));
        }
        TimeLimiter delegate = delegates.get(config.executor());
        if (delegate == null) {
            throw new IllegalStateException("No TimeLimiter configured for " + config.executor());
        }
        return delegate.execute(invocation, config);
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RuntimeException failure = null;
        Set<TimeLimiter> unique = new LinkedHashSet<>(delegates.values());
        for (TimeLimiter delegate : unique) {
            if (!(delegate instanceof AutoCloseable closeable)) {
                continue;
            }
            try {
                closeable.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close TimeLimiter", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
