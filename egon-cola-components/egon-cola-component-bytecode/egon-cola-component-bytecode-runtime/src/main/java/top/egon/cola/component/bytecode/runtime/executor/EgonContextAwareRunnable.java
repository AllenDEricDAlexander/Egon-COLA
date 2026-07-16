package top.egon.cola.component.bytecode.runtime.executor;

import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.Objects;

public final class EgonContextAwareRunnable implements Runnable, EgonInstrumentedTask {

    private final Runnable delegate;
    private final ExecutorTaskDecorator.TaskContext context;
    private final ExecutorTaskDecorator decorator;

    EgonContextAwareRunnable(
            Runnable delegate,
            ExecutorTaskDecorator.TaskContext context,
            ExecutorTaskDecorator decorator
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.context = Objects.requireNonNull(context, "context");
        this.decorator = Objects.requireNonNull(decorator, "decorator");
    }

    @Override
    public void run() {
        long startedNanos = System.nanoTime();
        decorator.started(context, startedNanos);
        try (ContextScope ignored = context.snapshot().restore()) {
            delegate.run();
        } catch (RuntimeException | Error failure) {
            decorator.failed(context, startedNanos, failure);
            throw failure;
        }
        decorator.completed(context, startedNanos);
    }
}
