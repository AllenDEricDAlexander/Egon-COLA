package top.egon.cola.component.gateway.core.exchange;

import top.egon.cola.component.gateway.contract.error.GatewayResult;

public interface GatewayResponse {

    GatewayResult result();

    GatewayHeaders headers();

    GatewayBody body();
}
