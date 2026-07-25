package top.egon.cola.component.gateway.core.filter;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;

public interface GatewayFilter {

    String id();

    GatewayFilterStage stage();

    int order();

    Publisher<GatewayResponse> filter(
            GatewayExchange exchange,
            GatewayFilterChain chain
    );
}
