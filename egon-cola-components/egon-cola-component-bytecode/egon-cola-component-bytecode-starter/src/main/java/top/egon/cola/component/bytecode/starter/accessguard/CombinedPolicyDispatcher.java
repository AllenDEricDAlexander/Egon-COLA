package top.egon.cola.component.bytecode.starter.accessguard;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeConstructorInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeMethodInvocation;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.ConstructorGuardDecision;
import top.egon.cola.component.bytecode.bridge.InvocationDecision;
import top.egon.cola.component.bytecode.bridge.ObservationToken;
import top.egon.cola.component.bytecode.runtime.accessguard.GuardedInvocationEvaluator;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public final class CombinedPolicyDispatcher implements BytecodeRuntimeDispatcher {

    private final BytecodeRuntimeDispatcher delegate;
    private final GuardedInvocationEvaluator accessGuardEvaluator;

    public CombinedPolicyDispatcher(
            BytecodeRuntimeDispatcher delegate,
            GuardedInvocationEvaluator accessGuardEvaluator
    ) {
        this.delegate = delegate;
        this.accessGuardEvaluator = accessGuardEvaluator;
    }

    @Override
    public int protocolMajor() {
        return delegate.protocolMajor();
    }

    @Override
    public int protocolMinor() {
        return delegate.protocolMinor();
    }

    @Override
    public Set<BridgeCapability> capabilities() {
        EnumSet<BridgeCapability> capabilities = delegate.capabilities().isEmpty()
                ? EnumSet.noneOf(BridgeCapability.class)
                : EnumSet.copyOf(delegate.capabilities());
        capabilities.add(BridgeCapability.ACCESS_GUARD);
        return Set.copyOf(capabilities);
    }

    @Override
    public Runnable decorateRunnable(
            Class<?> callerClass, Executor executor, Runnable task, long callSiteId) {
        return delegate.decorateRunnable(callerClass, executor, task, callSiteId);
    }

    @Override
    public <V> Callable<V> decorateCallable(
            Class<?> callerClass, Executor executor, Callable<V> task, long callSiteId) {
        return delegate.decorateCallable(callerClass, executor, task, callSiteId);
    }

    @Override
    public void executorRejected(
            Class<?> callerClass,
            long callSiteId,
            Executor executor,
            RejectedExecutionException exception
    ) {
        delegate.executorRejected(callerClass, callSiteId, executor, exception);
    }

    @Override
    public ObservationToken enterObservation(Class<?> declaringClass, long methodId) {
        return delegate.enterObservation(declaringClass, methodId);
    }

    @Override
    public void observationSuccess(ObservationToken token) {
        delegate.observationSuccess(token);
    }

    @Override
    public void observationError(ObservationToken token, Throwable throwable) {
        delegate.observationError(token, throwable);
    }

    @Override
    public void observationExit(ObservationToken token) {
        delegate.observationExit(token);
    }

    @Override
    public InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation) {
        return delegate.evaluateMethodExtension(invocation);
    }

    @Override
    public Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        return accessGuardEvaluator.invokeGuarded(invocation);
    }

    @Override
    public ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
        return accessGuardEvaluator.guardConstructor(invocation);
    }
}
