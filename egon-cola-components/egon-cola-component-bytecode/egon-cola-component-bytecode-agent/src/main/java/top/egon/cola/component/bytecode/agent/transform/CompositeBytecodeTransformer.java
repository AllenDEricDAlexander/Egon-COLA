package top.egon.cola.component.bytecode.agent.transform;

import top.egon.cola.component.bytecode.agent.AgentConfiguration;
import top.egon.cola.component.bytecode.agent.AgentFailure;
import top.egon.cola.component.bytecode.agent.AgentFailurePolicy;
import top.egon.cola.component.bytecode.agent.AgentState;
import top.egon.cola.component.bytecode.agent.AgentStateStore;
import top.egon.cola.component.bytecode.agent.ClassNameFilter;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CompositeBytecodeTransformer implements ClassFileTransformer {

    private final ClassNameFilter filter;
    private final AgentConfiguration configuration;
    private final AgentStateStore stateStore;
    private final TransformOperation operation;
    private final AtomicBoolean featureDisabled = new AtomicBoolean();

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
        if (featureDisabled.get() || !filter.matches(loader, className, classfileBuffer)) {
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
            switch (policy) {
                case MARK_FATAL -> stateStore.failed();
                case DISABLE_FEATURE -> disableEnhancement();
                case SKIP_CLASS -> degradeIfRunning();
            }
            return null;
        }
    }

    /**
     * Retires the enhancement for the rest of the JVM lifetime, leaving the process running. Later
     * classes are skipped without another transform attempt, which is what separates this policy
     * from SKIP_CLASS.
     */
    private void disableEnhancement() {
        if (!featureDisabled.compareAndSet(false, true)) {
            return;
        }
        for (BridgeCapability capability : configuration.features()) {
            stateStore.disableFeature(capability);
        }
        degradeIfRunning();
    }

    private void degradeIfRunning() {
        if (stateStore.state() == AgentState.ACTIVE
                || stateStore.state() == AgentState.STARTING) {
            stateStore.degraded();
        }
    }

    @FunctionalInterface
    public interface TransformOperation {
        byte[] transform(ClassLoader loader, String internalClassName, byte[] classfileBuffer)
                throws Exception;
    }
}
