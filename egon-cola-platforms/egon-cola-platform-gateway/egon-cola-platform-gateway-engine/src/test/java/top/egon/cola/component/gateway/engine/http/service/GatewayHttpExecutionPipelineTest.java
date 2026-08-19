package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayHttpExecutionPipelineTest {

    @Test
    void executesCorsSecurityGovernanceAndInvocationInCoreStageOrder() {
        List<String> calls = new ArrayList<>();
        GatewayHttpExecutionPipeline pipeline =
                new GatewayHttpExecutionPipeline();
        TestExchange exchange = new TestExchange(calls, false);

        GatewayOutboundHttpResponse response =
                pipeline.execute(exchange).block();

        assertEquals(200, response.status());
        assertEquals(
                List.of("cors", "security", "governance", "invocation"),
                calls
        );
    }

    @Test
    void corsMayShortCircuitBeforeSecurityAndInvocation() {
        List<String> calls = new ArrayList<>();
        GatewayHttpExecutionPipeline pipeline =
                new GatewayHttpExecutionPipeline();
        TestExchange exchange = new TestExchange(calls, true);

        GatewayOutboundHttpResponse response =
                pipeline.execute(exchange).block();

        assertEquals(204, response.status());
        assertEquals(List.of("cors"), calls);
    }

    private static final class TestExchange
            extends AbstractGatewayHttpStageExchange {

        private final List<String> calls;

        private final boolean preflight;

        private TestExchange(List<String> calls, boolean preflight) {
            super(GatewayHttpExchangeFixtures.request(), null);
            this.calls = calls;
            this.preflight = preflight;
        }

        @Override
        public Publisher<GatewayResponse> cors(GatewayFilterChain chain) {
            calls.add("cors");
            return preflight
                    ? respond(new GatewayOutboundHttpResponse(
                    204,
                    java.util.Map.of(),
                    reactor.core.publisher.Flux.empty()
            ))
                    : chain.filter(this);
        }

        @Override
        public Publisher<GatewayResponse> security(
                GatewayFilterChain chain) {
            calls.add("security");
            return chain.filter(this);
        }

        @Override
        public Publisher<GatewayResponse> governance(
                GatewayFilterChain chain) {
            calls.add("governance");
            return chain.filter(this);
        }

        @Override
        public Publisher<GatewayResponse> invoke() {
            calls.add("invocation");
            return respond(GatewayOutboundHttpResponse.text(200, "ok"));
        }

        @Override
        public GatewayOutboundHttpResponse mapFailure(Throwable failure) {
            return GatewayOutboundHttpResponse.text(500, "failed");
        }
    }

    private static final class GatewayHttpExchangeFixtures {

        private static GatewayInboundHttpRequest request() {
            return new GatewayInboundHttpRequest(
                    "GET",
                    "api.example.com",
                    "/orders",
                    java.util.Map.of(),
                    new java.net.InetSocketAddress("127.0.0.1", 12345),
                    reactor.core.publisher.Flux.empty()
            );
        }
    }
}
