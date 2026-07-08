package top.egon.cola.component.ruleengine.async;

import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultRuleAsyncExecutor implements RuleAsyncExecutor, AutoCloseable {

    private final ExecutorService executorService;

    public DefaultRuleAsyncExecutor(int corePoolSize, int maxPoolSize) {
        int size = Math.max(corePoolSize, maxPoolSize);
        this.executorService = Executors.newFixedThreadPool(Math.max(1, size));
    }

    @Override
    public <T> T load(Callable<T> loader, RuleContext context, Duration timeout) {
        Future<T> future = executorService.submit(loader);
        try {
            if (timeout == null) {
                return future.get();
            }
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            future.cancel(true);
            if (context != null) {
                context.addError(ex);
            }
            throw new IllegalStateException("rule async load failed", ex);
        }
    }

    @Override
    public <T> void loadToContext(String key, Callable<T> loader, RuleContext context, Duration timeout) {
        T value = load(loader, context, timeout);
        context.set(key, value);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    @Override
    public void close() {
        shutdown();
    }
}
