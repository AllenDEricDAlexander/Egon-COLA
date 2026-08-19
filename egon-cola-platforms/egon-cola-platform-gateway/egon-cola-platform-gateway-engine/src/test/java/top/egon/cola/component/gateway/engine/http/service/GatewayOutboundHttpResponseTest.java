package top.egon.cola.component.gateway.engine.http.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayOutboundHttpResponseTest {

    @Test
    void abandonmentIsIdempotentAcrossDerivedResponses() {
        AtomicInteger abandonments = new AtomicInteger();
        GatewayOutboundHttpResponse original =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(),
                        Flux.empty()
                ).onAbandon(abandonments::incrementAndGet);
        GatewayOutboundHttpResponse derived = original.withBody(
                        Flux.<DataBuffer>never()
                )
                .onAbandon(abandonments::incrementAndGet);

        derived.abandon();
        derived.abandon();
        original.abandon();

        assertEquals(2, abandonments.get());
    }
}
