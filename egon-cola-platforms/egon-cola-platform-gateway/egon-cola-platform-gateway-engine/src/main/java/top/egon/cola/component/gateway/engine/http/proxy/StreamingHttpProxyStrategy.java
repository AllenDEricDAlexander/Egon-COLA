package top.egon.cola.component.gateway.engine.http.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.GatewayRequestBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferPipeline;

public final class StreamingHttpProxyStrategy
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
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.limitBytes(
                                context.body(),
                                limit,
                                () -> new GatewayRequestBodyTooLargeException(
                                        "request body exceeds configured limit"
                                )
                        )
                );
        return context.adapter().invoke(new HttpUpstreamRequest(
                        context.provider(),
                        context.method(),
                        context.pathAndQuery(),
                        context.headers(),
                        body,
                        context.policy().connectTimeout(),
                        context.policy().responseHeaderTimeout(),
                        context.policy().streamIdleTimeout(),
                        context.policy().totalTimeout(),
                        false
                ))
                .map(response -> responseSemantics.apply(response, context));
    }
}
