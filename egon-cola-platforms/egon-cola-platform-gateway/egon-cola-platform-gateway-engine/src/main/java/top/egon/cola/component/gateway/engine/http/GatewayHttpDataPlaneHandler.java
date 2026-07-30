package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

@FunctionalInterface
public interface GatewayHttpDataPlaneHandler {

    Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request
    );
}
