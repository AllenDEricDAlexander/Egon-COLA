package top.egon.cola.component.bytecode.starter.methodextension;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeMethodInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.InvocationDecision;
import top.egon.cola.component.bytecode.runtime.methodextension.MethodExtensionInvocationEvaluator;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionNotReadyPolicy;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionResult;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class MethodExtensionRuntimeAdapter
        implements MethodExtensionInvocationEvaluator, BytecodeRuntimeDispatcher {

    private final Supplier<MethodExtensionExecutionService> executionServices;
    private final MethodMetadataResolver metadataResolver;
    private final MethodExtensionNotReadyPolicy notReadyPolicy;
    private volatile boolean ready;

    public MethodExtensionRuntimeAdapter(
            Supplier<MethodExtensionExecutionService> executionServices,
            MethodMetadataResolver metadataResolver,
            MethodExtensionNotReadyPolicy notReadyPolicy
    ) {
        this.executionServices = executionServices;
        this.metadataResolver = metadataResolver;
        this.notReadyPolicy = notReadyPolicy;
    }

    public void markReady() {
        ready = true;
    }

    @Override
    public InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation) {
        if (!ready) {
            return notReadyDecision();
        }
        MethodExtensionExecutionService executionService;
        try {
            executionService = executionServices.get();
        } catch (Throwable failure) {
            return InvocationDecision.throwing(failure);
        }
        if (executionService == null) {
            return notReadyDecision();
        }
        try {
            Method method = metadataResolver.resolve(
                    invocation.declaringClass(), invocation.methodId());
            MethodExtensionExecutionResult result = executionService.evaluate(
                    invocation.target(), method, invocation.arguments());
            if (result.proceed()) {
                return InvocationDecision.proceed();
            }
            return result.rejectionValue() == null
                    ? InvocationDecision.returnNull()
                    : InvocationDecision.returnValue(result.rejectionValue());
        } catch (Throwable failure) {
            return InvocationDecision.throwing(failure);
        }
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
        return Set.of(BridgeCapability.METHOD_EXTENSION);
    }

    @Override
    public Runnable decorateRunnable(
            Class<?> callerClass,
            Executor executor,
            Runnable task,
            long callSiteId
    ) {
        return task;
    }

    @Override
    public <V> Callable<V> decorateCallable(
            Class<?> callerClass,
            Executor executor,
            Callable<V> task,
            long callSiteId
    ) {
        return task;
    }

    private InvocationDecision notReadyDecision() {
        return switch (notReadyPolicy) {
            case PROCEED -> InvocationDecision.proceed();
            case REJECT -> InvocationDecision.returnNull();
            case FAIL -> InvocationDecision.throwing(new IllegalStateException(
                    "Method Extension Agent runtime is not ready"));
        };
    }
}
