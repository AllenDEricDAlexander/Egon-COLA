package top.egon.cola.component.bytecode.runtime;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public final class DefaultBytecodeRuntimeDispatcher implements BytecodeRuntimeDispatcher {

    private final ExecutorTaskDecorator taskDecorator;

    public DefaultBytecodeRuntimeDispatcher(ExecutorTaskDecorator taskDecorator) {
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator");
    }

    @Override
    public int protocolMajor() {
        return BridgeProtocol.MAJOR;
    }

    @Override
    public int protocolMinor() {
        return BridgeProtocol.MINOR;
    }

    @Override
    public Set<BridgeCapability> capabilities() {
        return Set.of(BridgeCapability.EXECUTOR);
    }

    @Override
    public Runnable decorateRunnable(
            Class<?> callerClass,
            Executor executor,
            Runnable task,
            long callSiteId
    ) {
        return taskDecorator.decorateRunnable(executor, task, callSiteId);
    }

    @Override
    public <V> Callable<V> decorateCallable(
            Class<?> callerClass,
            Executor executor,
            Callable<V> task,
            long callSiteId
    ) {
        return taskDecorator.decorateCallable(executor, task, callSiteId);
    }

    @Override
    public void executorRejected(
            Class<?> callerClass,
            long callSiteId,
            Executor executor,
            RejectedExecutionException exception
    ) {
        taskDecorator.rejected(executor, callSiteId, exception);
    }
}
