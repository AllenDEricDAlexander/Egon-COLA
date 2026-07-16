package top.egon.cola.component.bytecode.runtime.executor;

import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.Objects;
import java.util.concurrent.Callable;

public final class EgonContextAwareCallable<V> implements Callable<V>, EgonInstrumentedTask {

    private final Callable<V> delegate;
    private final ExecutorTaskDecorator.TaskContext context;
    private final ExecutorTaskDecorator decorator;

    EgonContextAwareCallable(
            Callable<V> delegate,
            ExecutorTaskDecorator.TaskContext context,
            ExecutorTaskDecorator decorator
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.context = Objects.requireNonNull(context, "context");
        this.decorator = Objects.requireNonNull(decorator, "decorator");
    }

    @Override
    public V call() throws Exception {
        long startedNanos = System.nanoTime();
        decorator.started(context, startedNanos);
        V result;
        try (ContextScope ignored = context.snapshot().restore()) {
            result = delegate.call();
        } catch (Exception | Error failure) {
            decorator.failed(context, startedNanos, failure);
            throw failure;
        }
        decorator.completed(context, startedNanos);
        return result;
    }
}
