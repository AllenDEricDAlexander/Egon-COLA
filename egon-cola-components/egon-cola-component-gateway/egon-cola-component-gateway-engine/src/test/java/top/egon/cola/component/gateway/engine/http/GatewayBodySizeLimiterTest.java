package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayBodySizeLimiterTest {

    @Test
    void rejectsRequestAcrossMultipleChunks() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();

        GatewayRequestBodyTooLargeException failure = assertThrows(
                GatewayRequestBodyTooLargeException.class,
                () -> limiter.aggregateRequest(
                        Flux.just(bytes("123"), bytes("456")),
                        5
                ).block()
        );

        assertEquals("GATEWAY_REQUEST_BODY_TOO_LARGE", failure.code());
    }

    @Test
    void rejectsResponseAcrossMultipleChunksWithoutAggregating() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(),
                        Flux.just(bytes("123"), bytes("456"))
                );

        GatewayResponseBodyTooLargeException failure = assertThrows(
                GatewayResponseBodyTooLargeException.class,
                () -> limiter.limitResponse(response, 5)
                        .body()
                        .collectList()
                        .block()
        );

        assertEquals("GATEWAY_RESPONSE_BODY_TOO_LARGE", failure.code());
    }

    @Test
    void rejectsDeclaredContentLengthBeforeBodySubscription() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of("content-length", List.of("6")),
                        Flux.never()
                );

        assertThrows(
                GatewayResponseBodyTooLargeException.class,
                () -> limiter.limitResponse(response, 5)
        );
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
