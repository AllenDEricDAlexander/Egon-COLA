package top.egon.cola.component.bytecode.runtime;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.ObservationToken;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;
import top.egon.cola.component.bytecode.runtime.observation.ObservationState;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public final class DefaultBytecodeRuntimeDispatcher implements BytecodeRuntimeDispatcher {

    private final ExecutorTaskDecorator taskDecorator;
    private final ObservationRuntime observationRuntime;

    public DefaultBytecodeRuntimeDispatcher(ExecutorTaskDecorator taskDecorator) {
        this(taskDecorator, null);
    }

    public DefaultBytecodeRuntimeDispatcher(
            ExecutorTaskDecorator taskDecorator,
            ObservationRuntime observationRuntime
    ) {
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator");
        this.observationRuntime = observationRuntime;
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
        return observationRuntime == null || !observationRuntime.enabled()
                ? Set.of(BridgeCapability.EXECUTOR)
                : Set.of(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION);
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

    @Override
    public ObservationToken enterObservation(Class<?> declaringClass, long methodId) {
        if (observationRuntime == null) {
            return ObservationToken.noop();
        }
        ObservationState state = observationRuntime.enter(declaringClass, methodId);
        return state == null ? ObservationToken.noop() : ObservationToken.active(this, state);
    }

    @Override
    public void observationSuccess(ObservationToken token) {
        if (observationRuntime != null && token.state() instanceof ObservationState state) {
            observationRuntime.success(state);
        }
    }

    @Override
    public void observationError(ObservationToken token, Throwable throwable) {
        if (observationRuntime != null && token.state() instanceof ObservationState state) {
            observationRuntime.error(state, throwable);
        }
    }

    @Override
    public void observationExit(ObservationToken token) {
        if (observationRuntime != null && token.state() instanceof ObservationState state) {
            observationRuntime.exit(state);
        }
    }
}
