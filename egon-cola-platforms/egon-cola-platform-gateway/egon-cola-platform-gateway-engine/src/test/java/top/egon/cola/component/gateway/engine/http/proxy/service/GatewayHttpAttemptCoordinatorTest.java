package top.egon.cola.component.gateway.engine.http.proxy.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.domain.GatewayRetryPolicy;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitGuard;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayHttpAttemptCoordinatorTest {

    @Test
    void openAiPostNeverRetriesEvenWhenLegacyPolicyEnablesRetry() {
        AtomicInteger attempts = new AtomicInteger();
        GatewayHttpAttemptCoordinator coordinator =
                new GatewayHttpAttemptCoordinator();

        assertThrows(RuntimeException.class, () -> coordinator.execute(
                openAi(),
                retryPolicy(),
                GatewayCommitGuard.http(),
                false,
                false,
                Duration.ofSeconds(1),
                () -> {
                    attempts.incrementAndGet();
                    return Mono.error(new IOException("connect"));
                },
                IOException.class::isInstance,
                ignored -> false
        ).block());

        assertEquals(1, attempts.get());
    }

    @Test
    void legacyReplayableRequestRetriesBeforeUpstreamHeaders() {
        AtomicInteger attempts = new AtomicInteger();
        String result = new GatewayHttpAttemptCoordinator().execute(
                EffectiveGatewayTransportPolicy.legacy(),
                retryPolicy(),
                GatewayCommitGuard.http(),
                true,
                true,
                Duration.ofSeconds(1),
                () -> attempts.incrementAndGet() == 1
                        ? Mono.error(new IOException("connect"))
                        : Mono.just("ok"),
                IOException.class::isInstance,
                ignored -> false
        ).block();

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    private GatewayRetryPolicy retryPolicy() {
        return new GatewayRetryPolicy(
                true,
                2,
                Duration.ZERO,
                Duration.ZERO,
                1,
                Duration.ofMillis(1),
                Set.of(503),
                Set.of()
        );
    }

    private EffectiveGatewayTransportPolicy openAi() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.SSE,
                1024,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Optional.of(Duration.ofSeconds(10)),
                Optional.empty(),
                OptionalLong.empty(),
                false,
                false,
                true
        );
    }
}
