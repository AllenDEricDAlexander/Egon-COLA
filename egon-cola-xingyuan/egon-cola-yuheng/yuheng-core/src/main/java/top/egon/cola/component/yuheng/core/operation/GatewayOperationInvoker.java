package top.egon.cola.component.yuheng.core.operation;

import org.reactivestreams.Publisher;

@FunctionalInterface
public interface GatewayOperationInvoker {

    Publisher<GatewayInvocationResult> invoke(
            GatewayOperationInvocation invocation);
}
