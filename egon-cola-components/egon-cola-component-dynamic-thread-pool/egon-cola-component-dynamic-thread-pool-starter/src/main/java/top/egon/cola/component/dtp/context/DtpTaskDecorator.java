package top.egon.cola.component.dtp.context;

import org.springframework.core.task.TaskDecorator;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import top.egon.cola.component.dtp.exception.DtpContextPropagationException;

/**
 * @author      有罗敷的马同学
 * @description DTP 任务装饰器
 * @Date        下午9:27 2026/6/29
 **/
public class DtpTaskDecorator implements TaskDecorator {

    private final Supplier<DtpTaskContextManager> contexts;

    /** 原无参构造继续仅捕获Trace，独立使用时不依赖Spring。 */
    public DtpTaskDecorator() { this.contexts = null; }

    public DtpTaskDecorator(ObjectProvider<DtpTaskContextManager> contexts) {
        this.contexts = Objects.requireNonNull(contexts, "contexts")::getIfAvailable;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (contexts == null) { return DtpRunnable.wrap(runnable); }
        if (runnable instanceof DtpTaskObserver.ObservedTask) { return runnable; }
        DtpTaskContextManager manager = contexts.get();
        if (manager == null) { return DtpRunnable.wrap(runnable); }
        DtpTaskContextBO context = manager.capture();
        return () -> {
            try (DtpTaskContextManager.Scope ignored = manager.open(context)) { runnable.run(); }
            catch (DtpContextPropagationException failure) {
                manager.diagnostic(failure, context.getTraceContext());
                // 外部Spring Future无法设置异常原因；未执行业务时取消原Future，避免永久等待。
                if (runnable instanceof Future<?> future
                        && !(runnable instanceof java.util.concurrent.CompletableFuture.AsynchronousCompletionTask)) {
                    future.cancel(false);
                    return;
                }
                throw failure;
            }
        };
    }
}
