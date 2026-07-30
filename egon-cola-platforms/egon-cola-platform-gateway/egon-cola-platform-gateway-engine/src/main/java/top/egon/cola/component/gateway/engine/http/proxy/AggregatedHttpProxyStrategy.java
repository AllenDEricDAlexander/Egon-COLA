package top.egon.cola.component.gateway.engine.http.proxy;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogDirection;

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
                                context.observeBody(
                                        Flux.defer(() -> Flux.just(
                                                DefaultDataBufferFactory
                                                        .sharedInstance
                                                        .wrap(body)
                                        )),
                                        GatewayBodyLogDirection.REQUEST,
                                        context.headers()
                                ),
                                context.policy().connectTimeout(),
                                context.policy().responseHeaderTimeout(),
                                context.policy().streamIdleTimeout(),
                                context.policy().totalTimeout(),
                                true
                        )
                ))
                .map(response -> responseSemantics.apply(response, context))
                .map(response -> response.withHeadersAndBody(
                        response.headers(),
                        context.observeBody(
                                response.body(),
                                GatewayBodyLogDirection.RESPONSE,
                                response.headers()
                        )
                ));
    }
}
