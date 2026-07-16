package top.egon.cola.component.bytecode.runtime.methodextension;

import top.egon.cola.component.bytecode.bridge.BridgeMethodInvocation;
import top.egon.cola.component.bytecode.bridge.InvocationDecision;

@FunctionalInterface
public interface MethodExtensionInvocationEvaluator {

    InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation);
}
