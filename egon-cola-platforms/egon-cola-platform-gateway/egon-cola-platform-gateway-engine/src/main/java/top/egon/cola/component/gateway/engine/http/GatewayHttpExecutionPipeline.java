package top.egon.cola.component.gateway.engine.http;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.execution.DefaultGatewayExecutor;
import top.egon.cola.component.gateway.core.execution.GatewayExecutor;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.DefaultGatewayFilterChain;
import top.egon.cola.component.gateway.core.filter.GatewayFilter;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.core.filter.GatewayFilterStage;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;

import java.util.List;

public final class GatewayHttpExecutionPipeline {

    private final GatewayExecutor executor;

    public GatewayHttpExecutionPipeline() {
        executor = new DefaultGatewayExecutor(
                new DefaultGatewayFilterChain(List.of(
                        filter(
                                "gateway-http-cors",
                                GatewayFilterStage.CORS,
                                (exchange, chain) -> exchange.cors(chain)
                        ),
                        filter(
                                "gateway-http-security",
                                GatewayFilterStage.AUTHENTICATION,
                                (exchange, chain) ->
                                        exchange.security(chain)
                        ),
                        filter(
                                "gateway-http-governance",
                                GatewayFilterStage.RATE_CONCURRENCY,
                                (exchange, chain) ->
                                        exchange.governance(chain)
                        ),
                        filter(
                                "gateway-http-invocation",
                                GatewayFilterStage.INVOCATION,
                                (exchange, chain) -> exchange.invoke()
                        )
                )),
                (exchange, failure) -> stage(exchange).fail(failure)
        );
    }

    public Mono<GatewayOutboundHttpResponse> execute(
            AbstractGatewayHttpStageExchange exchange) {
        return Mono.from(executor.execute(exchange))
                .map(ignored -> exchange.outbound());
    }

    public Mono<GatewayWebSocketHandshakeResult> executeWebSocket(
            AbstractGatewayHttpStageExchange exchange) {
        return Mono.from(executor.execute(exchange))
                .map(ignored -> exchange.webSocketResult());
    }

    private GatewayFilter filter(
            String id,
            GatewayFilterStage stage,
            StageInvocation invocation) {
        return new GatewayFilter() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public GatewayFilterStage stage() {
                return stage;
            }

            @Override
            public int order() {
                return 0;
            }

            @Override
            public Publisher<GatewayResponse> filter(
                    GatewayExchange exchange,
                    GatewayFilterChain chain) {
                return invocation.invoke(
                        GatewayHttpExecutionPipeline.stage(exchange),
                        chain
                );
            }
        };
    }

    private static AbstractGatewayHttpStageExchange stage(
            GatewayExchange exchange) {
        if (exchange instanceof AbstractGatewayHttpStageExchange staged) {
            return staged;
        }
        throw new IllegalArgumentException(
                "gateway HTTP pipeline requires a staged HTTP exchange"
        );
    }

    @FunctionalInterface
    private interface StageInvocation {

        Publisher<GatewayResponse> invoke(
                AbstractGatewayHttpStageExchange exchange,
                GatewayFilterChain chain);
    }
}
