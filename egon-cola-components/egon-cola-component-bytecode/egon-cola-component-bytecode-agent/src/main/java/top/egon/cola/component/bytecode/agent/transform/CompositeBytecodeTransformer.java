package top.egon.cola.component.bytecode.agent.transform;

import top.egon.cola.component.bytecode.agent.AgentConfiguration;
import top.egon.cola.component.bytecode.agent.AgentFailure;
import top.egon.cola.component.bytecode.agent.AgentFailurePolicy;
import top.egon.cola.component.bytecode.agent.AgentState;
import top.egon.cola.component.bytecode.agent.AgentStateStore;
import top.egon.cola.component.bytecode.agent.ClassNameFilter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public final class CompositeBytecodeTransformer implements ClassFileTransformer {

    private final ClassNameFilter filter;
    private final AgentConfiguration configuration;
    private final AgentStateStore stateStore;
    private final TransformOperation operation;

    public CompositeBytecodeTransformer(
            ClassNameFilter filter,
            AgentConfiguration configuration,
            AgentStateStore stateStore,
            TransformOperation operation
    ) {
        this.filter = filter;
        this.configuration = configuration;
        this.stateStore = stateStore;
        this.operation = operation;
    }

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) throws IllegalClassFormatException {
        if (!filter.matches(loader, className, classfileBuffer)) {
            stateStore.skipped();
            return null;
        }
        try {
            byte[] transformed = operation.transform(loader, className, classfileBuffer);
            if (transformed != null) {
                stateStore.transformed();
            } else {
                stateStore.skipped();
            }
            return transformed;
        } catch (Throwable failure) {
            AgentFailurePolicy policy = configuration.failurePolicy();
            stateStore.recordFailure(AgentFailure.transform(
                    loader, className, "COMPOSITE", failure, policy));
            stateStore.skipped();
            if (policy == AgentFailurePolicy.MARK_FATAL) {
                stateStore.failed();
                IllegalClassFormatException exception = new IllegalClassFormatException(
                        "Egon bytecode transformation failed for " + className);
                exception.initCause(failure);
                throw exception;
            }
            if (stateStore.state() == AgentState.ACTIVE
                    || stateStore.state() == AgentState.STARTING) {
                stateStore.degraded();
            }
            return null;
        }
    }

    @FunctionalInterface
    public interface TransformOperation {
        byte[] transform(ClassLoader loader, String internalClassName, byte[] classfileBuffer)
                throws Exception;
    }
}
