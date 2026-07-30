package top.egon.cola.component.gateway.engine.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.proxy.GatewayHttpAttemptCoordinator;
import top.egon.cola.component.gateway.engine.traffic.GatewayRetryPolicy;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayNoRetryAfterCommitTest {

    @ParameterizedTest
    @ValueSource(strings = {"CONNECT", "HEADER", "502", "503"})
    void openAiPostUsesOneAttemptForEveryPreCommitFailure(
            String failureKind) {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> execute(
                openAiHttp(
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.SSE,
                        true
                ),
                GatewayCommitGuard.http(),
                attempts,
                !failureKind.matches("5\\d\\d"),
                failureKind.matches("5\\d\\d")
        ));

        assertEquals(1, attempts.get());
    }

    @Test
    void streamingCannotRetryEvenWhenTheRouteOverrideEnablesIt() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> execute(
                openAiHttp(
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.STANDARD,
                        true
                ),
                GatewayCommitGuard.http(),
                attempts,
                true,
                false
        ));

        assertEquals(1, attempts.get());
    }

    @ParameterizedTest
    @EnumSource(value = GatewayCommitPoint.class, names = {
            "UPSTREAM_HEADERS_RECEIVED",
            "DOWNSTREAM_HEADERS_COMMITTED",
            "FIRST_BODY_BUFFER_SENT"
    })
    void legacyHttpCannotRetryAfterHeadersOrPayloadCommit(
            GatewayCommitPoint commitPoint) {
        GatewayCommitGuard guard = GatewayCommitGuard.http();
        guard.advance(commitPoint);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> execute(
                EffectiveGatewayTransportPolicy.legacy(),
                guard,
                attempts,
                true,
                false
        ));

        assertEquals(1, attempts.get());
    }

    @ParameterizedTest
    @EnumSource(value = GatewayCommitPoint.class, names = {
            "UPSTREAM_HANDSHAKE_RECEIVED",
            "CLIENT_HANDSHAKE_COMMITTED",
            "FIRST_FRAME_FORWARDED"
    })
    void websocketCannotRetryAfterHandshakeOrFrameCommit(
            GatewayCommitPoint commitPoint) {
        GatewayCommitGuard guard = GatewayCommitGuard.websocket();
        guard.advance(commitPoint);
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> execute(
                websocket(),
                guard,
                attempts,
                true,
                true
        ));

        assertEquals(1, attempts.get());
    }

    private void execute(
            EffectiveGatewayTransportPolicy policy,
            GatewayCommitGuard guard,
            AtomicInteger attempts,
            boolean transportFailure,
            boolean statusFailure) {
        new GatewayHttpAttemptCoordinator().execute(
                policy,
                retryPolicy(),
                guard,
                true,
                true,
                Duration.ofSeconds(1),
                () -> {
                    attempts.incrementAndGet();
                    return Mono.error(new IOException("upstream failure"));
                },
                ignored -> transportFailure,
                ignored -> statusFailure
        ).block(Duration.ofSeconds(2));
    }

    private GatewayRetryPolicy retryPolicy() {
        return new GatewayRetryPolicy(
                true,
                3,
                Duration.ZERO,
                Duration.ZERO,
                1,
                Duration.ofMillis(1),
                Set.of(502, 503),
                Set.of()
        );
    }

    private EffectiveGatewayTransportPolicy openAiHttp(
            GatewayRequestBodyMode requestMode,
            GatewayTransportResponseMode responseMode,
            boolean retryAllowed) {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                requestMode,
                responseMode,
                1024,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Optional.of(Duration.ofSeconds(5)),
                Optional.empty(),
                OptionalLong.empty(),
                false,
                retryAllowed,
                true
        );
    }

    private EffectiveGatewayTransportPolicy websocket() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                1024,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Optional.empty(),
                Optional.of(Duration.ofSeconds(5)),
                OptionalLong.of(64 * 1024),
                false,
                true,
                true
        );
    }
}
