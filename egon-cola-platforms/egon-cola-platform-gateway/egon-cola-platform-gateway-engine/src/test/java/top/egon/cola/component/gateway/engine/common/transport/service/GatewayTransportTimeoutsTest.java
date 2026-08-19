package top.egon.cola.component.gateway.engine.common.transport.service;

import top.egon.cola.component.gateway.engine.common.transport.service.GatewayConnectTimeoutException;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayResponseHeaderTimeoutException;
import top.egon.cola.component.gateway.engine.common.transport.domain.GatewayStreamDirection;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayStreamIdleTimeoutException;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayTotalTimeoutException;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayWebSocketIdleTimeoutException;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;

class GatewayTransportTimeoutsTest {

    @Test
    void reportsConnectAndResponseHeaderTimeoutsSeparately() {
        StepVerifier.withVirtualTime(() -> GatewayTransportTimeouts.connect(
                        Mono.never(), Duration.ofSeconds(1)
                ))
                .thenAwait(Duration.ofSeconds(1))
                .expectError(GatewayConnectTimeoutException.class)
                .verify();

        StepVerifier.withVirtualTime(() ->
                        GatewayTransportTimeouts.responseHeaders(
                                Mono.never(), Duration.ofSeconds(2)
                        ))
                .thenAwait(Duration.ofSeconds(2))
                .expectError(GatewayResponseHeaderTimeoutException.class)
                .verify();
    }

    @Test
    void distinguishesStreamTotalAndWebsocketIdleTimeouts() {
        StepVerifier.withVirtualTime(() ->
                        GatewayTransportTimeouts.responseIdle(
                                Flux.never(), Duration.ofSeconds(1)
                        ))
                .thenAwait(Duration.ofSeconds(1))
                .expectErrorSatisfies(failure -> {
                    assert failure instanceof GatewayStreamIdleTimeoutException;
                    assert ((GatewayStreamIdleTimeoutException) failure)
                            .direction()
                            == GatewayStreamDirection.RESPONSE;
                })
                .verify();

        StepVerifier.withVirtualTime(() -> GatewayTransportTimeouts.total(
                        Flux.never(),
                        Optional.of(Duration.ofSeconds(2))
                ))
                .thenAwait(Duration.ofSeconds(2))
                .expectError(GatewayTotalTimeoutException.class)
                .verify();

        StepVerifier.withVirtualTime(() ->
                        GatewayTransportTimeouts.websocketIdle(
                                Flux.never(), Duration.ofSeconds(3)
                        ))
                .thenAwait(Duration.ofSeconds(3))
                .expectError(GatewayWebSocketIdleTimeoutException.class)
                .verify();
    }
}
