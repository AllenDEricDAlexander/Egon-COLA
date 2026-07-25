package top.egon.cola.component.gateway.core.exchange;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

public interface GatewayRequest {

    String requestId();

    String traceId();

    GatewayProtocol protocol();

    AccessZone accessZone();

    GatewayHeaders headers();

    GatewayBody body();
}
