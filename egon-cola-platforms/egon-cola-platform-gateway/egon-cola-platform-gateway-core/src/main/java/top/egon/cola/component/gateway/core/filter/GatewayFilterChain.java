package top.egon.cola.component.gateway.core.filter;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;

@FunctionalInterface
public interface GatewayFilterChain {

    Publisher<GatewayResponse> filter(GatewayExchange exchange);
}
