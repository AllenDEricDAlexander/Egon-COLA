package top.egon.cola.component.gateway.contract.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceCallPolicyCodecTest {

    private static GatewayRuntimePolicy policy(String type, Map<String, Object> configuration) {
        return new GatewayRuntimePolicy("p-" + type.toLowerCase(), type, "OPERATION", configuration);
    }

    private static ServiceCallPolicy decode(GatewayRuntimePolicy... policies) {
        return ServiceCallPolicyCodec.decode(List.of(policies));
    }

    @Nested
    @DisplayName("unknown keys survive an edit")
    class UnknownKeysSurvive {

        @Test
        @DisplayName("a key this type does not model is preserved through decode and encode")
        void preservesUnmodelledKeys() {
            // Two admin versions editing the same policy must not delete each other's settings.
            GatewayRuntimePolicy existing = policy(ServiceCallPolicyCodec.TYPE_RETRY, Map.of(
                    "maxAttempts", 3,
                    "someFutureKnob", "keep-me"));

            List<GatewayRuntimePolicy> encoded = ServiceCallPolicyCodec.encode(
                    ServiceCallPolicyCodec.decode(List.of(existing)), List.of(existing), type -> "new-id");

            Map<String, Object> retry = encoded.stream()
                    .filter(p -> p.type().equals(ServiceCallPolicyCodec.TYPE_RETRY))
                    .findFirst().orElseThrow().configuration();
            assertEquals("keep-me", retry.get("someFutureKnob"));
            assertEquals(3, retry.get("maxAttempts"));
        }

        @Test
        @DisplayName("an existing policy keeps its id and scope instead of being recreated")
        void preservesIdentity() {
            GatewayRuntimePolicy existing = new GatewayRuntimePolicy(
                    "retry-original", ServiceCallPolicyCodec.TYPE_RETRY, "ROUTE", Map.of());

            GatewayRuntimePolicy encoded = ServiceCallPolicyCodec.encode(
                            ServiceCallPolicy.defaults(), List.of(existing), type -> "generated")
                    .stream().filter(p -> p.type().equals(ServiceCallPolicyCodec.TYPE_RETRY))
                    .findFirst().orElseThrow();

            assertEquals("retry-original", encoded.policyId());
            assertEquals("ROUTE", encoded.scope());
        }

        @Test
        @DisplayName("a policy type absent before gets a generated id")
        void generatesIdForNewType() {
            GatewayRuntimePolicy encoded = ServiceCallPolicyCodec.encode(
                            ServiceCallPolicy.defaults(), List.of(), type -> "gen-" + type)
                    .stream().filter(p -> p.type().equals(ServiceCallPolicyCodec.TYPE_CACHE))
                    .findFirst().orElseThrow();

            assertEquals("gen-CACHE", encoded.policyId());
        }
    }

    @Nested
    @DisplayName("decoding is total")
    class DecodingIsTotal {

        @Test
        @DisplayName("no policies yields the defaults")
        void emptyYieldsDefaults() {
            assertSame(ServiceCallPolicy.defaults(), ServiceCallPolicyCodec.decode(List.of()));
            assertSame(ServiceCallPolicy.defaults(), ServiceCallPolicyCodec.decode(null));
        }

        @Test
        @DisplayName("a malformed number falls back rather than making the rule set unreadable")
        void malformedNumbersFallBack() {
            ServiceCallPolicy decoded = decode(policy(ServiceCallPolicyCodec.TYPE_RETRY,
                    Map.of("maxAttempts", "not-a-number", "multiplier", "nope")));

            assertEquals(2, decoded.retry().maxAttempts());
            assertEquals(2.0d, decoded.retry().multiplier());
        }

        @Test
        @DisplayName("an out-of-range attempt count falls back instead of throwing")
        void outOfRangeAttemptsFallBack() {
            assertEquals(2, decode(policy(ServiceCallPolicyCodec.TYPE_RETRY,
                    Map.of("maxAttempts", 99))).retry().maxAttempts());
        }

        @Test
        @DisplayName("backoff ordering is repaired rather than rejected")
        void repairsInvertedBackoff() {
            // The record forbids maximum < initial; a published rule that violates it must
            // still decode, so the codec normalises instead of propagating the failure.
            ServiceCallPolicy decoded = decode(policy(ServiceCallPolicyCodec.TYPE_RETRY,
                    Map.of("initialBackoff", 500, "maximumBackoff", 10)));

            assertEquals(Duration.ofMillis(500), decoded.retry().initialBackoff());
            assertEquals(Duration.ofMillis(500), decoded.retry().maximumBackoff());
        }

        @Test
        @DisplayName("consistent hash without a hash key degrades instead of failing to decode")
        void consistentHashWithoutKeyDegrades() {
            ServiceCallPolicy decoded = decode(policy(ServiceCallPolicyCodec.TYPE_LOAD_BALANCE,
                    Map.of("strategy", "CONSISTENT_HASH")));

            assertEquals(LoadBalanceStrategy.SMOOTH_WEIGHTED_ROUND_ROBIN, decoded.loadBalance().strategy());
        }

        @Test
        @DisplayName("minimumNumberOfCalls is capped to the window so the breaker can evaluate")
        void capsMinimumCallsToWindow() {
            ServiceCallPolicy decoded = decode(policy(ServiceCallPolicyCodec.TYPE_CIRCUIT_BREAKER,
                    Map.of("slidingWindowSize", 5, "minimumNumberOfCalls", 50)));

            assertEquals(5, decoded.circuitBreaker().minimumNumberOfCalls());
        }

        @Test
        @DisplayName("an unrecognised strategy name falls back to the default")
        void unknownStrategyFallsBack() {
            assertEquals(LoadBalanceStrategy.SMOOTH_WEIGHTED_ROUND_ROBIN,
                    decode(policy(ServiceCallPolicyCodec.TYPE_LOAD_BALANCE,
                            Map.of("strategy", "MAGIC"))).loadBalance().strategy());
        }
    }

    @Nested
    @DisplayName("duration parsing accepts every form the engine tolerates")
    class DurationForms {

        @Test
        @DisplayName("a bare number is milliseconds")
        void numberIsMillis() {
            assertEquals(Duration.ofMillis(250),
                    decode(policy(ServiceCallPolicyCodec.TYPE_TIMEOUT, Map.of("timeout", 250))).timeout());
        }

        @Test
        @DisplayName("an ISO-8601 string is parsed")
        void isoStringIsParsed() {
            assertEquals(Duration.ofSeconds(5),
                    decode(policy(ServiceCallPolicyCodec.TYPE_TIMEOUT, Map.of("timeout", "PT5S"))).timeout());
        }

        @Test
        @DisplayName("a sibling Millis key is honoured")
        void millisSiblingIsHonoured() {
            assertEquals(Duration.ofMillis(750),
                    decode(policy(ServiceCallPolicyCodec.TYPE_TIMEOUT,
                            Map.of("timeoutMillis", 750))).timeout());
        }

        @Test
        @DisplayName("an unparseable string falls back")
        void unparseableFallsBack() {
            assertEquals(ServiceCallPolicy.DEFAULT_TIMEOUT,
                    decode(policy(ServiceCallPolicyCodec.TYPE_TIMEOUT,
                            Map.of("timeout", "five seconds"))).timeout());
        }
    }

    @Nested
    @DisplayName("round trip")
    class RoundTrip {

        @Test
        @DisplayName("every modelled field survives encode then decode")
        void roundTrips() {
            ServiceCallPolicy original = new ServiceCallPolicy(
                    Duration.ofSeconds(7),
                    new ServiceCallPolicy.RetryPolicy(true, 4, Duration.ofMillis(25),
                            Duration.ofMillis(400), 3.0d, Duration.ofMillis(50),
                            Set.of(500, 503), Set.of("UNAVAILABLE"), false),
                    new ServiceCallPolicy.LoadBalancePolicy(LoadBalanceStrategy.LEAST_IN_FLIGHT, "", false),
                    new ServiceCallPolicy.CircuitBreakerPolicy(true, 75f, 40, 20,
                            Duration.ofSeconds(60), 5),
                    new ServiceCallPolicy.CachePolicy(true, Duration.ofMinutes(5), "#path",
                            Set.of("accept-language")));

            ServiceCallPolicy decoded = ServiceCallPolicyCodec.decode(
                    ServiceCallPolicyCodec.encode(original, List.of(), type -> "id-" + type));

            assertEquals(original, decoded);
        }
    }

    @Nested
    @DisplayName("safety constraints")
    class SafetyConstraints {

        @Test
        @DisplayName("retry is suppressed on a non-idempotent operation by default")
        void retrySuppressedOnNonIdempotent() {
            ServiceCallPolicy.RetryPolicy retry = ServiceCallPolicy.RetryPolicy.defaults();

            assertTrue(retry.appliesTo(true));
            assertFalse(retry.appliesTo(false));
        }

        @Test
        @DisplayName("clearing the idempotency guard is what allows a non-idempotent retry")
        void guardCanBeCleared() {
            ServiceCallPolicy.RetryPolicy retry = new ServiceCallPolicy.RetryPolicy(
                    true, 3, Duration.ofMillis(10), Duration.ofMillis(100), 2.0d,
                    Duration.ofMillis(20), Set.of(503), Set.of("UNAVAILABLE"), false);

            assertTrue(retry.appliesTo(false));
        }

        @Test
        @DisplayName("the unsafe combination is reported for the management UI")
        void reportsUnsafeCombination() {
            ServiceCallPolicy policy = new ServiceCallPolicy(
                    Duration.ofSeconds(3),
                    new ServiceCallPolicy.RetryPolicy(true, 3, Duration.ofMillis(10),
                            Duration.ofMillis(100), 2.0d, Duration.ofMillis(20),
                            Set.of(503), Set.of("UNAVAILABLE"), false),
                    ServiceCallPolicy.LoadBalancePolicy.defaults(),
                    ServiceCallPolicy.CircuitBreakerPolicy.defaults(),
                    ServiceCallPolicy.CachePolicy.disabled());

            assertTrue(policy.unsafeReason(false).orElseThrow().contains("duplicate its side effect"));
            assertTrue(policy.unsafeReason(true).isEmpty());
        }

        @Test
        @DisplayName("only read-shaped methods are cacheable")
        void onlyReadMethodsAreCacheable() {
            assertTrue(ServiceCallPolicy.CachePolicy.cacheableRequestMethod("GET"));
            assertTrue(ServiceCallPolicy.CachePolicy.cacheableRequestMethod("head"));
            assertFalse(ServiceCallPolicy.CachePolicy.cacheableRequestMethod("POST"));
        }

        @Test
        @DisplayName("consistent hash without a hash key is rejected at construction")
        void consistentHashRequiresKey() {
            assertThrows(IllegalArgumentException.class, () -> new ServiceCallPolicy.LoadBalancePolicy(
                    LoadBalanceStrategy.CONSISTENT_HASH, "", true));
        }

        @Test
        @DisplayName("a breaker that could never evaluate is rejected")
        void rejectsUnevaluableBreaker() {
            assertThrows(IllegalArgumentException.class, () -> new ServiceCallPolicy.CircuitBreakerPolicy(
                    true, 50f, 10, 20, Duration.ofSeconds(30), 3));
        }
    }
}
