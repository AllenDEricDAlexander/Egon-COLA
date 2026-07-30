package top.egon.cola.component.gateway.core.execution;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.error.GatewayError;
import top.egon.cola.component.gateway.contract.error.GatewayErrorCategory;
import top.egon.cola.component.gateway.contract.error.GatewayResult;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.exchange.AggregatedGatewayBody;
import top.egon.cola.component.gateway.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayHeaders;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultGatewayExecutorTest {

    @Test
    void mapsAsynchronousFailureAndAlwaysReleasesBody() {
        AggregatedGatewayBody body = new AggregatedGatewayBody(
                new byte[]{1},
                1,
                false
        );
        StubExchange exchange = new StubExchange(body);
        DefaultGatewayExecutor executor = new DefaultGatewayExecutor(
                ignored -> Mono.error(new IllegalArgumentException("boom")),
                (ignored, error) -> new DefaultGatewayResponse(
                        GatewayResult.failure(new GatewayError(
                                "GATEWAY_INTERNAL_ERROR",
                                GatewayErrorCategory.INTERNAL_ERROR,
                                "Gateway request failed",
                                "trace",
                                false,
                                Map.of()
                        )),
                        ImmutableGatewayHeaders.empty(),
                        body
                )
        );

        GatewayResponse response = Mono.from(executor.execute(exchange)).block();

        assertEquals(
                "GATEWAY_INTERNAL_ERROR",
                response.result().error().orElseThrow().code()
        );
        assertTrue(body.closed());
    }

    @Test
    void preventsRepeatedExecutionOfSameExchange() {
        AggregatedGatewayBody body = new AggregatedGatewayBody(
                new byte[0],
                0,
                true
        );
        StubExchange exchange = new StubExchange(body);
        DefaultGatewayExecutor executor = new DefaultGatewayExecutor(
                ignored -> Mono.just(exchange.response()),
                (ignored, error) -> exchange.response()
        );

        Mono.from(executor.execute(exchange)).block();

        assertThrows(
                IllegalStateException.class,
                () -> Mono.from(executor.execute(exchange)).block()
        );
    }

    private record StubExchange(AggregatedGatewayBody body)
            implements GatewayExchange {

        @Override
        public GatewayRequest request() {
            return new GatewayRequest() {
                @Override
                public String requestId() {
                    return "request";
                }

                @Override
                public String traceId() {
                    return "trace";
                }

                @Override
                public GatewayProtocol protocol() {
                    return GatewayProtocol.HTTP;
                }

                @Override
                public AccessZone accessZone() {
                    return AccessZone.INTERNAL;
                }

                @Override
                public GatewayHeaders headers() {
                    return ImmutableGatewayHeaders.empty();
                }

                @Override
                public AggregatedGatewayBody body() {
                    return body;
                }
            };
        }

        @Override
        public GatewayContext context() {
            return null;
        }

        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(body);
        }
    }
}
