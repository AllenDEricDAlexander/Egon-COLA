package top.egon.cola.component.gateway.engine.transport;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRetryGateTest {

    private final GatewayRetryGate gate = new GatewayRetryGate();

    @Test
    void allowsOnlyUncommittedReplayableLegacyTransportFailures() {
        GatewayCommitGuard guard = GatewayCommitGuard.http();

        assertTrue(gate.canRetryTransportFailure(
                legacyPolicy(), guard, true, true, true, 1, 2
        ));
        guard.advance(GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED);
        assertFalse(gate.canRetryTransportFailure(
                legacyPolicy(), guard, true, true, true, 1, 2
        ));
    }

    @Test
    void permitsNamedLegacyStatusRetryOnlyBeforeDownstreamCommit() {
        GatewayCommitGuard guard = GatewayCommitGuard.http();
        guard.advance(GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED);

        assertTrue(gate.canRetryLegacyStatus(
                legacyPolicy(), guard, true, true, true, 1, 2
        ));
        guard.advance(GatewayCommitPoint.DOWNSTREAM_HEADERS_COMMITTED);
        assertFalse(gate.canRetryLegacyStatus(
                legacyPolicy(), guard, true, true, true, 1, 2
        ));
    }

    @Test
    void neverRetriesOpenAiStreamingMultipartBinaryOrWebsocketModes() {
        for (EffectiveGatewayTransportPolicy policy : new EffectiveGatewayTransportPolicy[]{
                policy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.SSE
                ),
                policy(
                        GatewayRouteProfile.DEFAULT,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.STANDARD
                ),
                policy(
                        GatewayRouteProfile.DEFAULT,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.AGGREGATED,
                        GatewayTransportResponseMode.BINARY_STREAM
                ),
                policy(
                        GatewayRouteProfile.DEFAULT,
                        GatewayTransportProtocol.WEBSOCKET,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.AUTO_STREAM
                )
        }) {
            GatewayCommitGuard guard = policy.transportProtocol()
                    == GatewayTransportProtocol.WEBSOCKET
                    ? GatewayCommitGuard.websocket()
                    : GatewayCommitGuard.http();
            assertFalse(gate.canRetryTransportFailure(
                    policy, guard, true, true, true, 1, 2
            ));
            assertFalse(gate.canRetryLegacyStatus(
                    policy, guard, true, true, true, 1, 2
            ));
        }
    }

    private EffectiveGatewayTransportPolicy legacyPolicy() {
        return policy(
                GatewayRouteProfile.DEFAULT,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.AGGREGATED,
                GatewayTransportResponseMode.STANDARD
        );
    }

    private EffectiveGatewayTransportPolicy policy(
            GatewayRouteProfile profile,
            GatewayTransportProtocol transport,
            GatewayRequestBodyMode requestMode,
            GatewayTransportResponseMode responseMode) {
        boolean websocket = transport == GatewayTransportProtocol.WEBSOCKET;
        return new EffectiveGatewayTransportPolicy(
                profile,
                transport,
                requestMode,
                responseMode,
                1024,
                OptionalLong.of(2048),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Optional.of(Duration.ofSeconds(4)),
                websocket
                        ? Optional.of(Duration.ofSeconds(5))
                        : Optional.empty(),
                websocket ? OptionalLong.of(4096) : OptionalLong.empty(),
                false,
                true,
                false
        );
    }
}
