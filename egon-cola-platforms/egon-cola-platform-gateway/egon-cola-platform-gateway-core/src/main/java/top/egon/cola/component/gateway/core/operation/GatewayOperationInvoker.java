package top.egon.cola.component.gateway.core.operation;

import org.reactivestreams.Publisher;

@FunctionalInterface
public interface GatewayOperationInvoker {

    Publisher<GatewayInvocationResult> invoke(
            GatewayOperationInvocation invocation);
}
