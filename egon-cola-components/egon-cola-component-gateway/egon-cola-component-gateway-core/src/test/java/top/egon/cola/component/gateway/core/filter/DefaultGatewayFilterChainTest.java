package top.egon.cola.component.gateway.core.filter;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultGatewayFilterChainTest {

    @Test
    void ordersFiltersByFixedStageThenOrder() {
        List<String> calls = new ArrayList<>();
        GatewayFilter observation = filter(
                "observation",
                GatewayFilterStage.OBSERVATION,
                0,
                calls
        );
        GatewayFilter exposure = filter(
                "exposure",
                GatewayFilterStage.EXPOSURE,
                10,
                calls
        );
        GatewayFilter cors = filter(
                "cors",
                GatewayFilterStage.CORS,
                0,
                calls
        );
        StubExchange exchange = new StubExchange();

        Mono.from(new DefaultGatewayFilterChain(List.of(
                observation,
                cors,
                exposure
        )).filter(exchange)).block();

        assertEquals(
                List.of("exposure", "cors", "observation"),
                calls
        );
    }

    @Test
    void rejectsDuplicateStageOrder() {
        List<String> calls = new ArrayList<>();
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultGatewayFilterChain(List.of(
                        filter(
                                "one",
                                GatewayFilterStage.CORS,
                                0,
                                calls
                        ),
                        filter(
                                "two",
                                GatewayFilterStage.CORS,
                                0,
                                calls
                        )
                ))
        );
    }

    private GatewayFilter filter(
            String id,
            GatewayFilterStage stage,
            int order,
            List<String> calls) {
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
                return order;
            }

            @Override
            public org.reactivestreams.Publisher<GatewayResponse> filter(
                    GatewayExchange exchange,
                    GatewayFilterChain chain) {
                calls.add(id);
                return chain.filter(exchange);
            }
        };
    }

    private static final class StubExchange implements GatewayExchange {

        @Override
        public top.egon.cola.component.gateway.core.exchange.GatewayRequest
        request() {
            return null;
        }

        @Override
        public top.egon.cola.component.gateway.core.context.GatewayContext
        context() {
            return null;
        }

        @Override
        public GatewayResponse response() {
            return new top.egon.cola.component.gateway.core.exchange
                    .DefaultGatewayResponse(
                    top.egon.cola.component.gateway.contract.error
                            .GatewayResult.success(),
                    top.egon.cola.component.gateway.core.exchange
                            .ImmutableGatewayHeaders.empty(),
                    top.egon.cola.component.gateway.core.exchange
                            .EmptyGatewayBody.INSTANCE
            );
        }
    }
}
