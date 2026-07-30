package top.egon.cola.component.gateway.engine.http.proxy;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;

@FunctionalInterface
public interface GatewayHttpProxyStrategy {

    Mono<GatewayOutboundHttpResponse> proxy(GatewayHttpProxyContext context);
}
