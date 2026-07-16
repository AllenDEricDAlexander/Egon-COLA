package top.egon.cola.component.bytecode.runtime.accessguard;

import top.egon.cola.component.bytecode.bridge.BridgeConstructorInvocation;
import top.egon.cola.component.bytecode.bridge.BridgeGuardedInvocation;
import top.egon.cola.component.bytecode.bridge.ConstructorGuardDecision;

public interface GuardedInvocationEvaluator {

    Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable;

    default ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
        return ConstructorGuardDecision.allow();
    }
}
