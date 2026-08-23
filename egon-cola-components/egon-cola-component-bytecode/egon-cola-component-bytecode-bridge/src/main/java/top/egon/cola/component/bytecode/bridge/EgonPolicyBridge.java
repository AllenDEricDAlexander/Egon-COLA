package top.egon.cola.component.bytecode.bridge;

public final class EgonPolicyBridge {

    private EgonPolicyBridge() {
    }

    public static InvocationDecision evaluateMethodExtension(
            Object target,
            Class<?> declaringClass,
            long methodId,
            Object[] arguments
    ) {
        BytecodeRuntimeDispatcher dispatcher = DispatcherRegistry
                .dispatcher(declaringClass, BridgeCapability.METHOD_EXTENSION)
                .orElse(null);
        if (dispatcher == null) {
            return InvocationDecision.proceed();
        }
        try {
            InvocationDecision decision = dispatcher.evaluateMethodExtension(
                    new BridgeMethodInvocation(target, declaringClass, methodId, arguments));
            return decision == null ? InvocationDecision.proceed() : decision;
        } catch (Throwable ignored) {
            return InvocationDecision.proceed();
        }
    }

}
