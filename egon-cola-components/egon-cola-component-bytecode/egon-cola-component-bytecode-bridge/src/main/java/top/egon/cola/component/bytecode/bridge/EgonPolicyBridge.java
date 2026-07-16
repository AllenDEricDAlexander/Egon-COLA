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

    public static Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        BytecodeRuntimeDispatcher dispatcher = DispatcherRegistry
                .dispatcher(invocation.declaringClass(), BridgeCapability.ACCESS_GUARD)
                .orElse(null);
        return dispatcher == null
                ? invocation.proceed() : dispatcher.invokeGuarded(invocation);
    }

    public static ConstructorGuardDecision guardConstructor(
            Class<?> declaringClass,
            long methodId,
            Object[] arguments,
            BridgeFailHint failHint
    ) {
        BridgeConstructorInvocation invocation = new BridgeConstructorInvocation(
                declaringClass, methodId, arguments, failHint);
        BytecodeRuntimeDispatcher dispatcher = DispatcherRegistry
                .dispatcher(declaringClass, BridgeCapability.ACCESS_GUARD)
                .orElse(null);
        if (dispatcher == null) {
            return ConstructorGuardDecision.allow();
        }
        try {
            ConstructorGuardDecision decision = dispatcher.guardConstructor(invocation);
            return decision == null ? ConstructorGuardDecision.allow() : decision;
        } catch (Throwable failure) {
            return failHint == BridgeFailHint.FAIL_CLOSED
                    ? ConstructorGuardDecision.throwing(failure)
                    : ConstructorGuardDecision.allow();
        }
    }
}
