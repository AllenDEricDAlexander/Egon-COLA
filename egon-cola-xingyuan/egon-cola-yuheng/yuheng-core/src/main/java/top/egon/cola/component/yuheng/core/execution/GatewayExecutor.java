package top.egon.cola.component.yuheng.core.execution;

import org.reactivestreams.Publisher;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.exchange.GatewayResponse;

public interface GatewayExecutor {

    Publisher<GatewayResponse> execute(GatewayExchange exchange);
}
