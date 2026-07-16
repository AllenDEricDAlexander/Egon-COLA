package top.egon.cola.component.bytecode.bridge;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public interface BytecodeRuntimeDispatcher {

    int protocolMajor();

    int protocolMinor();

    Set<BridgeCapability> capabilities();

    Runnable decorateRunnable(Class<?> callerClass, Executor executor, Runnable task, long callSiteId);

    <V> Callable<V> decorateCallable(
            Class<?> callerClass,
            Executor executor,
            Callable<V> task,
            long callSiteId
    );

    default void executorRejected(
            Class<?> callerClass,
            long callSiteId,
            Executor executor,
            RejectedExecutionException exception
    ) {
    }

    default ObservationToken enterObservation(Class<?> declaringClass, long methodId) {
        return ObservationToken.noop();
    }

    default void observationSuccess(ObservationToken token) {
    }

    default void observationError(ObservationToken token, Throwable throwable) {
    }

    default void observationExit(ObservationToken token) {
    }

    default InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation) {
        return InvocationDecision.proceed();
    }

    default Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        return invocation.proceed();
    }

    default ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
        return ConstructorGuardDecision.allow();
    }
}
