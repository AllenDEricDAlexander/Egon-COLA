package top.egon.cola.component.ruleengine.async;

import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultRuleAsyncExecutor implements RuleAsyncExecutor, AutoCloseable {

    private final ExecutorService executorService;

    /**
     * Grows from {@code corePoolSize} to {@code maxPoolSize} under concurrency. The handoff queue is
     * deliberately synchronous: a queue absorbs tasks and the pool then never reaches its maximum,
     * which is what previously made the core size inert. Work beyond the maximum runs on the calling
     * thread, which already blocks in {@link #load}.
     */
    public DefaultRuleAsyncExecutor(int corePoolSize, int maxPoolSize) {
        int core = Math.max(1, corePoolSize);
        int max = Math.max(core, maxPoolSize);
        this.executorService = new ThreadPoolExecutor(
                core, max, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());
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
