package top.egon.cola.component.yuheng.core.exchange;

import top.egon.cola.component.yuheng.core.context.GatewayContext;

public interface GatewayExchange {

    GatewayRequest request();

    GatewayContext context();

    GatewayResponse response();
}
