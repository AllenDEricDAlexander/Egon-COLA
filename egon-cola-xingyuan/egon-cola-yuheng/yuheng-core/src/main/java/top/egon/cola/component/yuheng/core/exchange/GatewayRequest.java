package top.egon.cola.component.yuheng.core.exchange;

import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;

public interface GatewayRequest {

    String requestId();

    String traceId();

    GatewayProtocol protocol();

    AccessZone accessZone();

    GatewayHeaders headers();

    GatewayBody body();
}
