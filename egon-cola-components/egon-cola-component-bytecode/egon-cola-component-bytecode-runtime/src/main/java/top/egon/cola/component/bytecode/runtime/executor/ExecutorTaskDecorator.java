package top.egon.cola.component.bytecode.runtime.executor;

import top.egon.cola.component.bytecode.api.executor.ExecutorEvent;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.context.ContextSnapshot;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public final class ExecutorTaskDecorator {

    private final CompositeContextCarrier contextCarrier;
    private final RuntimeEventFanout eventFanout;
    private final RuntimeTaskDetector taskDetector;
    private final ExecutorNameResolver nameResolver;

    public ExecutorTaskDecorator(
            CompositeContextCarrier contextCarrier,
            RuntimeEventFanout eventFanout,
            RuntimeTaskDetector taskDetector,
            ExecutorNameResolver nameResolver
    ) {
        this.contextCarrier = Objects.requireNonNull(contextCarrier, "contextCarrier");
        this.eventFanout = Objects.requireNonNull(eventFanout, "eventFanout");
        this.taskDetector = Objects.requireNonNull(taskDetector, "taskDetector");
        this.nameResolver = Objects.requireNonNull(nameResolver, "nameResolver");
    }

    public Runnable decorateRunnable(Executor executor, Runnable task, long callSiteId) {
        Objects.requireNonNull(task, "task");
        if (taskDetector.instrumented(task)) {
            return task;
        }
        TaskContext context = capture(executor, callSiteId);
        return new EgonContextAwareRunnable(task, context, this);
    }

    public <V> Callable<V> decorateCallable(
            Executor executor,
            Callable<V> task,
            long callSiteId
    ) {
        Objects.requireNonNull(task, "task");
        if (taskDetector.instrumented(task)) {
            return task;
        }
        TaskContext context = capture(executor, callSiteId);
        return new EgonContextAwareCallable<>(task, context, this);
    }

    public void rejected(
            Executor executor,
            long callSiteId,
            RejectedExecutionException exception
    ) {
        long now = System.nanoTime();
        eventFanout.publish(event(
                callSiteId,
                nameResolver.resolve(executor),
                executor.getClass().getName(),
                "REJECTED",
                "REJECTED",
                exceptionGroup(exception),
                now,
                0L,
                now
        ));
    }

    void started(TaskContext context, long startedNanos) {
        eventFanout.publish(event(
                context.callSiteId,
                context.executorName,
                context.executorType,
                "STARTED",
                "RUNNING",
                "NONE",
                context.submittedNanos,
                startedNanos,
                0L
        ));
    }

    void completed(TaskContext context, long startedNanos) {
        eventFanout.publish(event(
                context.callSiteId,
                context.executorName,
                context.executorType,
                "COMPLETED",
                "SUCCESS",
                "NONE",
                context.submittedNanos,
                startedNanos,
                System.nanoTime()
        ));
    }

    void failed(TaskContext context, long startedNanos, Throwable failure) {
        eventFanout.publish(event(
                context.callSiteId,
                context.executorName,
                context.executorType,
                "FAILED",
                "ERROR",
                exceptionGroup(failure),
                context.submittedNanos,
                startedNanos,
                System.nanoTime()
        ));
    }

    private TaskContext capture(Executor executor, long callSiteId) {
        Objects.requireNonNull(executor, "executor");
        long submittedNanos = System.nanoTime();
        TaskContext context = new TaskContext(
                callSiteId,
                nameResolver.resolve(executor),
                executor.getClass().getName(),
                submittedNanos,
                contextCarrier.capture()
        );
        eventFanout.publish(event(
                callSiteId,
                context.executorName,
                context.executorType,
                "SUBMITTED",
                "PENDING",
                "NONE",
                submittedNanos,
                0L,
                0L
        ));
        return context;
    }

    private ExecutorEvent event(
            long callSiteId,
            String executorName,
            String executorType,
            String phase,
            String result,
            String exceptionGroup,
            long submittedNanos,
            long startedNanos,
            long completedNanos
    ) {
        return new ExecutorEvent(
                callSiteId,
                executorName,
                executorType,
                phase,
                result,
                exceptionGroup,
                Thread.currentThread().isVirtual(),
                submittedNanos,
                startedNanos,
                completedNanos
        );
    }

    private String exceptionGroup(Throwable throwable) {
        return throwable == null ? "NONE" : throwable.getClass().getSimpleName();
    }

    record TaskContext(
            long callSiteId,
            String executorName,
            String executorType,
            long submittedNanos,
            ContextSnapshot snapshot
    ) {
    }
}
