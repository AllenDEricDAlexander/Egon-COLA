package top.egon.cola.component.methodextension.handler;

import top.egon.cola.component.methodextension.context.MethodExtensionContext;

public interface MethodExtensionHandler {

    MethodExtensionDecision evaluate(MethodExtensionContext context) throws Exception;
}
