package top.egon.cola.component.gateway.core.execution;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;

public interface GatewayExecutor {

    Publisher<GatewayResponse> execute(GatewayExchange exchange);
}
