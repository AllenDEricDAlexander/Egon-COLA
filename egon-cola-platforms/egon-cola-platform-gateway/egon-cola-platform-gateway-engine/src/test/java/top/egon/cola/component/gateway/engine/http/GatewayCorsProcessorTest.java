package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.engine.cors.GatewayCorsPolicyCompiler;
import top.egon.cola.component.gateway.engine.cors.RuntimeCorsPolicy;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCorsProcessorTest {

    @Test
    void handlesPreflightBeforeCallingTheRemainingPipeline() {
        GatewayCorsProcessor processor = processor(Map.of(
                "cors-public",
                policy(
                        List.of("https://app.example.com"),
                        List.of("GET", "POST"),
                        List.of("content-type", "x-request-id"),
                        true
                )
        ));
        GatewayInboundHttpRequest request = request(
                "OPTIONS",
                Map.of(
                        "origin", List.of("https://app.example.com"),
                        "access-control-request-method", List.of("POST"),
                        "access-control-request-headers",
                        List.of("Content-Type, X-Request-Id")
                )
        );

        GatewayCorsProcessor.Decision decision = processor.evaluate(
                Set.of("cors-public"),
                request,
                "POST",
                "trace"
        );
        GatewayOutboundHttpResponse response =
                decision.preflightResponse().orElseThrow();

        assertEquals(204, response.status());
        assertEquals(
                List.of("https://app.example.com"),
                response.headers().get("access-control-allow-origin")
        );
        assertEquals(
                List.of("true"),
                response.headers().get("access-control-allow-credentials")
        );
    }

    @Test
    void decoratesActualResponseWithoutAggregatingItsBody() {
        GatewayCorsProcessor processor = processor(Map.of(
                "cors-public",
                policy(
                        List.of("https://app.example.com"),
                        List.of("GET"),
                        List.of("content-type"),
                        false
                )
        ));
        GatewayCorsProcessor.Decision decision = processor.evaluate(
                Set.of("cors-public"),
                request(
                        "GET",
                        Map.of(
                                "origin",
                                List.of("https://app.example.com")
                        )
                ),
                "GET",
                "trace"
        );
        GatewayOutboundHttpResponse response = decision.decorate(
                GatewayOutboundHttpResponse.text(200, "ok")
        );

        assertTrue(decision.preflightResponse().isEmpty());
        assertEquals(
                List.of("https://app.example.com"),
                response.headers().get("access-control-allow-origin")
        );
        assertEquals(
                "ok",
                GatewayDataBufferTestSupport.joinUtf8(response.body(), 16)
        );
    }

    @Test
    void rejectsUnlistedOrigin() {
        GatewayCorsProcessor processor = processor(Map.of(
                "cors-public",
                policy(
                        List.of("https://app.example.com"),
                        List.of("GET"),
                        List.of(),
                        false
                )
        ));

        GatewayCorsException failure = assertThrows(
                GatewayCorsException.class,
                () -> processor.evaluate(
                        Set.of("cors-public"),
                        request(
                                "GET",
                                Map.of(
                                        "origin",
                                        List.of("https://evil.example.com")
                                )
                        ),
                        "GET",
                        "trace"
                )
        );

        assertEquals("GATEWAY_CORS_REJECTED", failure.code());
    }

    @Test
    void compilerRejectsWildcardOriginWithCredentials() {
        GatewayRuntimePolicy source = new GatewayRuntimePolicy(
                "cors-public",
                "CORS",
                "OPERATION",
                Map.of(
                        "allowedOrigins", List.of("*"),
                        "allowedMethods", List.of("GET"),
                        "allowCredentials", true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayCorsPolicyCompiler().compile(
                        List.of(source)
                )
        );
    }

    private GatewayCorsProcessor processor(
            Map<String, RuntimeCorsPolicy> policies) {
        return new GatewayCorsProcessor(() -> policies);
    }

    private RuntimeCorsPolicy policy(
            List<String> origins,
            List<String> methods,
            List<String> headers,
            boolean credentials) {
        return new GatewayCorsPolicyCompiler().compile(List.of(
                new GatewayRuntimePolicy(
                        "cors-public",
                        "CORS",
                        "OPERATION",
                        Map.of(
                                "allowedOrigins", origins,
                                "allowedMethods", methods,
                                "allowedHeaders", headers,
                                "exposedHeaders", List.of(
                                        "traceparent",
                                        "x-egon-request-id"
                                ),
                                "allowCredentials", credentials,
                                "maxAgeSeconds", 600
                        )
                )
        )).get("cors-public");
    }

    private GatewayInboundHttpRequest request(
            String method,
            Map<String, List<String>> headers) {
        return new GatewayInboundHttpRequest(
                method,
                "api.example.com",
                "/orders",
                headers,
                new InetSocketAddress("127.0.0.1", 12345),
                reactor.core.publisher.Flux.empty()
        );
    }
}
