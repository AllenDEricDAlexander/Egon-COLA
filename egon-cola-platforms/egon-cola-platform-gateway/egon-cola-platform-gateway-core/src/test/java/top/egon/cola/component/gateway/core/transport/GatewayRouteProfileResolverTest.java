package top.egon.cola.component.gateway.core.transport;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRouteProfileResolverTest {

    private static final long MIB = 1024L * 1024L;
    private static final GatewayTransportDefaults LEGACY_DEFAULTS =
            new GatewayTransportDefaults(
                    2L * MIB,
                    OptionalLong.of(4L * MIB),
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(5),
                    Optional.empty(),
                    false,
                    true
            );
    private static final GatewayTransportSafetyLimits SAFETY =
            new GatewayTransportSafetyLimits(
                    1024L * MIB,
                    Duration.ofSeconds(60),
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(30),
                    Duration.ofHours(2),
                    Duration.ofHours(2),
                    64L * MIB
            );
    private static final GatewayTransportPolicyOverrides NO_OVERRIDES =
            GatewayTransportPolicyOverrides.none();

    private final GatewayRouteProfileResolver resolver =
            new GatewayRouteProfileResolver();

    @TestFactory
    Stream<DynamicTest> resolvesCompleteProfileDefaults() {
        List<ProfileCase> cases = List.of(
                new ProfileCase("missing policy uses legacy defaults", null,
                        legacyExpected()),
                new ProfileCase("explicit default uses legacy defaults",
                        profileOnly(GatewayRouteProfile.DEFAULT),
                        legacyExpected()),
                new ProfileCase("openai profile uses streaming defaults",
                        profileOnly(GatewayRouteProfile.OPENAI_HTTP),
                        openAiExpected())
        );

        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.name(),
                () -> assertEquals(
                        testCase.expected(),
                        resolver.resolve(
                                testCase.policy(),
                                LEGACY_DEFAULTS,
                                NO_OVERRIDES,
                                SAFETY
                        )
                )
        ));
    }

    @Test
    void routeOverridesWinBeforeTrafficLimitsAndExplicitTotalWins() {
        GatewayRouteTransportPolicy route = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                GatewayRequestBodyMode.AGGREGATED,
                GatewayTransportResponseMode.STANDARD,
                64L * MIB,
                20_000L,
                180_000L,
                240_000L,
                2_700_000L,
                1_800_000L,
                32L * MIB,
                true,
                true
        );
        GatewayTransportPolicyOverrides traffic =
                new GatewayTransportPolicyOverrides(
                        OptionalLong.of(32L * MIB),
                        OptionalLong.of(8L * MIB),
                        Optional.of(Duration.ofMinutes(10))
                );

        EffectiveGatewayTransportPolicy effective = resolver.resolve(
                route,
                LEGACY_DEFAULTS,
                traffic,
                SAFETY
        );

        assertEquals(GatewayRouteProfile.OPENAI_HTTP, effective.profile());
        assertEquals(GatewayTransportProtocol.WEBSOCKET,
                effective.transportProtocol());
        assertEquals(GatewayRequestBodyMode.AGGREGATED,
                effective.requestBodyMode());
        assertEquals(GatewayTransportResponseMode.STANDARD,
                effective.responseMode());
        assertEquals(32L * MIB, effective.maxRequestBodyBytes());
        assertEquals(OptionalLong.of(8L * MIB),
                effective.maxResponseBodyBytes());
        assertEquals(Duration.ofSeconds(20), effective.connectTimeout());
        assertEquals(Duration.ofMinutes(3),
                effective.responseHeaderTimeout());
        assertEquals(Duration.ofMinutes(4), effective.streamIdleTimeout());
        assertEquals(Duration.ofMinutes(45),
                effective.totalTimeout().orElseThrow());
        assertEquals(Duration.ofMinutes(30),
                effective.websocketIdleTimeout().orElseThrow());
        assertEquals(OptionalLong.of(32L * MIB),
                effective.websocketMaxFrameBytes());
        assertTrue(effective.bodyLogEnabled());
        assertTrue(effective.retryAllowed());
        assertTrue(effective.authorizationForwardingAllowed());
    }

    @Test
    void openAiWebsocketUsesProfileIdleAndFrameDefaults() {
        GatewayRouteTransportPolicy route = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        EffectiveGatewayTransportPolicy effective = resolver.resolve(
                route,
                LEGACY_DEFAULTS,
                NO_OVERRIDES,
                SAFETY
        );

        assertEquals(Duration.ofMinutes(5),
                effective.websocketIdleTimeout().orElseThrow());
        assertEquals(OptionalLong.of(16L * MIB),
                effective.websocketMaxFrameBytes());
    }

    @Test
    void trafficOverridesApplyWhenRouteDoesNotSetTotalTimeout() {
        GatewayTransportPolicyOverrides traffic =
                new GatewayTransportPolicyOverrides(
                        OptionalLong.of(128L * MIB),
                        OptionalLong.of(8L * MIB),
                        Optional.of(Duration.ofMinutes(6))
                );

        EffectiveGatewayTransportPolicy effective = resolver.resolve(
                profileOnly(GatewayRouteProfile.OPENAI_HTTP),
                LEGACY_DEFAULTS,
                traffic,
                SAFETY
        );

        assertEquals(128L * MIB, effective.maxRequestBodyBytes());
        assertEquals(OptionalLong.of(8L * MIB),
                effective.maxResponseBodyBytes());
        assertEquals(Duration.ofMinutes(6),
                effective.totalTimeout().orElseThrow());
    }

    @Test
    void acceptsEverySpecifiedHardBoundary() {
        GatewayRouteTransportPolicy atBoundary = numericPolicy(
                1024L * MIB,
                60_000L,
                600_000L,
                1_800_000L,
                7_200_000L,
                7_200_000L,
                64L * MIB
        );

        EffectiveGatewayTransportPolicy effective = resolver.resolve(
                atBoundary,
                LEGACY_DEFAULTS,
                NO_OVERRIDES,
                SAFETY
        );

        assertEquals(1024L * MIB, effective.maxRequestBodyBytes());
        assertEquals(Duration.ofSeconds(60), effective.connectTimeout());
        assertEquals(Duration.ofMinutes(10),
                effective.responseHeaderTimeout());
        assertEquals(Duration.ofMinutes(30), effective.streamIdleTimeout());
        assertEquals(Duration.ofHours(2),
                effective.totalTimeout().orElseThrow());
        assertEquals(Duration.ofHours(2),
                effective.websocketIdleTimeout().orElseThrow());
        assertEquals(OptionalLong.of(64L * MIB),
                effective.websocketMaxFrameBytes());
    }

    @TestFactory
    Stream<DynamicTest> rejectsValuesBeyondSpecifiedHardBoundaries() {
        List<InvalidCase> cases = List.of(
                invalid("request body above 1 GiB", numericPolicy(
                        1024L * MIB + 1, 60_000L, 600_000L, 1_800_000L,
                        7_200_000L, 7_200_000L, 64L * MIB)),
                invalid("connect timeout above 60 seconds", numericPolicy(
                        1024L * MIB, 60_001L, 600_000L, 1_800_000L,
                        7_200_000L, 7_200_000L, 64L * MIB)),
                invalid("header timeout above 10 minutes", numericPolicy(
                        1024L * MIB, 60_000L, 600_001L, 1_800_000L,
                        7_200_000L, 7_200_000L, 64L * MIB)),
                invalid("idle timeout above 30 minutes", numericPolicy(
                        1024L * MIB, 60_000L, 600_000L, 1_800_001L,
                        7_200_000L, 7_200_000L, 64L * MIB)),
                invalid("total timeout above 2 hours", numericPolicy(
                        1024L * MIB, 60_000L, 600_000L, 1_800_000L,
                        7_200_001L, 7_200_000L, 64L * MIB)),
                invalid("websocket idle above 2 hours", numericPolicy(
                        1024L * MIB, 60_000L, 600_000L, 1_800_000L,
                        7_200_000L, 7_200_001L, 64L * MIB)),
                invalid("websocket frame above 64 MiB", numericPolicy(
                        1024L * MIB, 60_000L, 600_000L, 1_800_000L,
                        7_200_000L, 7_200_000L, 64L * MIB + 1))
        );

        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.name(),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                testCase.policy(),
                                LEGACY_DEFAULTS,
                                NO_OVERRIDES,
                                SAFETY
                        )
                )
        ));
    }

    @TestFactory
    Stream<DynamicTest> rejectsValuesBelowSpecifiedMinimums() {
        List<InvalidCase> cases = List.of(
                invalid("request body below one byte", numericPolicy(
                        0L, 100L, 1_000L, 1_000L, 1_000L, 1_000L, 1024L)),
                invalid("connect timeout below 100 milliseconds", numericPolicy(
                        1L, 99L, 1_000L, 1_000L, 1_000L, 1_000L, 1024L)),
                invalid("header timeout below one second", numericPolicy(
                        1L, 100L, 999L, 1_000L, 1_000L, 1_000L, 1024L)),
                invalid("idle timeout below one second", numericPolicy(
                        1L, 100L, 1_000L, 999L, 1_000L, 1_000L, 1024L)),
                invalid("total timeout below one second", numericPolicy(
                        1L, 100L, 1_000L, 1_000L, 999L, 1_000L, 1024L)),
                invalid("websocket idle below one second", numericPolicy(
                        1L, 100L, 1_000L, 1_000L, 1_000L, 999L, 1024L)),
                invalid("websocket frame below 1 KiB", numericPolicy(
                        1L, 100L, 1_000L, 1_000L, 1_000L, 1_000L, 1023L))
        );

        return cases.stream().map(testCase -> DynamicTest.dynamicTest(
                testCase.name(),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                testCase.policy(),
                                LEGACY_DEFAULTS,
                                NO_OVERRIDES,
                                SAFETY
                        )
                )
        ));
    }

    @Test
    void resolvingTheSameInputsIsStable() {
        GatewayRouteTransportPolicy route = profileOnly(
                GatewayRouteProfile.OPENAI_HTTP
        );

        EffectiveGatewayTransportPolicy first = resolver.resolve(
                route,
                LEGACY_DEFAULTS,
                NO_OVERRIDES,
                SAFETY
        );
        EffectiveGatewayTransportPolicy second = resolver.resolve(
                route,
                LEGACY_DEFAULTS,
                NO_OVERRIDES,
                SAFETY
        );

        assertEquals(first, second);
        assertTrue(first.authorizationForwardingAllowed());
        assertFalse(legacyExpected().authorizationForwardingAllowed());
    }

    private EffectiveGatewayTransportPolicy legacyExpected() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.DEFAULT,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.AGGREGATED,
                GatewayTransportResponseMode.STANDARD,
                2L * MIB,
                OptionalLong.of(4L * MIB),
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Optional.empty(),
                Optional.empty(),
                OptionalLong.empty(),
                false,
                true,
                false
        );
    }

    private EffectiveGatewayTransportPolicy openAiExpected() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                512L * MIB,
                OptionalLong.empty(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(120),
                Duration.ofSeconds(90),
                Optional.of(Duration.ofMinutes(30)),
                Optional.empty(),
                OptionalLong.empty(),
                false,
                false,
                true
        );
    }

    private GatewayRouteTransportPolicy profileOnly(
            GatewayRouteProfile profile) {
        return new GatewayRouteTransportPolicy(
                profile,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private GatewayRouteTransportPolicy numericPolicy(
            long requestBytes,
            long connectMs,
            long responseHeaderMs,
            long streamIdleMs,
            long totalMs,
            long websocketIdleMs,
            long websocketFrameBytes) {
        return new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                null,
                null,
                requestBytes,
                connectMs,
                responseHeaderMs,
                streamIdleMs,
                totalMs,
                websocketIdleMs,
                websocketFrameBytes,
                null,
                null
        );
    }

    private InvalidCase invalid(
            String name,
            GatewayRouteTransportPolicy policy) {
        return new InvalidCase(name, policy);
    }

    private record ProfileCase(
            String name,
            GatewayRouteTransportPolicy policy,
            EffectiveGatewayTransportPolicy expected) {
    }

    private record InvalidCase(
            String name,
            GatewayRouteTransportPolicy policy) {
    }
}
