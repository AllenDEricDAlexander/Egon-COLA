package top.egon.cola.component.yuheng.core.filter;

import org.reactivestreams.Publisher;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.exchange.GatewayResponse;

@FunctionalInterface
public interface GatewayFilterChain {

    Publisher<GatewayResponse> filter(GatewayExchange exchange);
}
