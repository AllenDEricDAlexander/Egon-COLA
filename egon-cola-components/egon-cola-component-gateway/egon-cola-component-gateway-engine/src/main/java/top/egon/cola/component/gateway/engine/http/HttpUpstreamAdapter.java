package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface HttpUpstreamAdapter {

    Mono<GatewayOutboundHttpResponse> invoke(HttpUpstreamRequest request);
}
