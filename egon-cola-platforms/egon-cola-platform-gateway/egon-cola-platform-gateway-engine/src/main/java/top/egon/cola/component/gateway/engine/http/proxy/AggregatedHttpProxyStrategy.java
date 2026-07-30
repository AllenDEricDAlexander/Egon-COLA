package top.egon.cola.component.gateway.engine.http.proxy;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;

public final class AggregatedHttpProxyStrategy
        implements GatewayHttpProxyStrategy {

    private final GatewayBodySizeLimiter limiter =
            new GatewayBodySizeLimiter();

    private final GatewayHttpResponseSemantics responseSemantics =
            new GatewayHttpResponseSemantics(limiter);

    @Override
    public Mono<GatewayOutboundHttpResponse> proxy(
            GatewayHttpProxyContext context) {
        long limit = context.policy().maxRequestBodyBytes();
        limiter.validateRequestHeaders(context.headers(), limit);
        return limiter.aggregateRequest(context.body(), limit)
                .flatMap(body -> context.adapter().invoke(
                        new HttpUpstreamRequest(
                                context.provider(),
                                context.method(),
                                context.pathAndQuery(),
                                context.headers(),
                                Flux.defer(() -> Flux.just(
                                        DefaultDataBufferFactory.sharedInstance
                                                .wrap(body)
                                )),
                                context.policy().responseHeaderTimeout(),
                                true
                        )
                ))
                .map(response -> responseSemantics.apply(response, context));
    }
}
