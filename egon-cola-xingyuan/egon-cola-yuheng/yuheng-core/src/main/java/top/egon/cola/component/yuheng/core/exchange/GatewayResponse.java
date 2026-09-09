package top.egon.cola.component.yuheng.core.exchange;

import top.egon.cola.component.yuheng.contract.error.GatewayResult;

public interface GatewayResponse {

    GatewayResult result();

    GatewayHeaders headers();

    GatewayBody body();
}
