package top.egon.cola.component.gateway.core.exchange;

import top.egon.cola.component.gateway.core.context.GatewayContext;

public interface GatewayExchange {

    GatewayRequest request();

    GatewayContext context();

    GatewayResponse response();
}
