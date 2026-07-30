package top.egon.cola.component.ddc.management.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceInstanceMetaCodecTest {

    @Nested
    @DisplayName("wire compatibility")
    class WireCompatibility {

        @Test
        @DisplayName("an all-default instance contributes no metadata at all")
        void defaultsEncodeToNothing() {
            // The compatibility guarantee: a provider that adopts this type without setting
            // anything registers byte-identically to one built before the type existed.
            assertTrue(ServiceInstanceMetaCodec.encode(ServiceInstanceMeta.defaults()).isEmpty());
        }

        @Test
        @DisplayName("metadata written before health reporting decodes to defaults")
        void legacyMetadataDecodesToDefaults() {
            Map<String, String> legacy = Map.of("gateway.weight", "250", "team", "payments");

            ServiceInstanceMeta meta = ServiceInstanceMetaCodec.decode(legacy);

            assertEquals(250, meta.weight());
            assertEquals(InstanceHealthState.UNKNOWN, meta.healthState());
            assertNull(meta.lastHealthCheckAt());
        }

        @Test
        @DisplayName("empty metadata returns the shared defaults instance")
        void emptyMetadataReturnsDefaults() {
            assertSame(ServiceInstanceMeta.defaults(), ServiceInstanceMetaCodec.decode(Map.of()));
            assertSame(ServiceInstanceMeta.defaults(), ServiceInstanceMetaCodec.decode(null));
        }

        @Test
        @DisplayName("every field survives an encode/decode round trip")
        void roundTrips() {
            ServiceInstanceMeta original = new ServiceInstanceMeta(
                    750, "cn-east", "cn-east-1a",
                    Map.of("canary", "true", "batch", "b7"),
                    "h2", "def-123", "5.2.3", "build-99", "/actuator",
                    120, InstanceHealthState.DEGRADED, Instant.parse("2026-07-26T10:15:30Z"));

            ServiceInstanceMeta decoded = ServiceInstanceMetaCodec.decode(
                    ServiceInstanceMetaCodec.encode(original));

            assertEquals(original, decoded);
        }

        @Test
        @DisplayName("encoded output passes the strict write-path validator")
        void encodedOutputIsValid() {
            // encode() and validate() must agree, otherwise a provider could build a valid
            // object that the registration layer then rejects.
            ServiceInstanceMeta meta = new ServiceInstanceMeta(
                    9999, "eu-west", "eu-west-2b", Map.of("tier", "gold"),
                    "grpc", "def-9", "1.0.0", "b1", "/mgmt",
                    30, InstanceHealthState.UP, Instant.parse("2026-01-01T00:00:00Z"));

            ServiceInstanceMetaCodec.validateAll(ServiceInstanceMetaCodec.encode(meta));
        }
    }

    @Nested
    @DisplayName("decoding is tolerant")
    class DecodingIsTolerant {

        @Test
        @DisplayName("a malformed weight falls back to the default instead of throwing")
        void malformedWeightFallsBack() {
            // Decode runs on the routing hot path against data another process wrote.
            // Throwing here would drop the whole service out of discovery.
            ServiceInstanceMeta meta = ServiceInstanceMetaCodec.decode(
                    Map.of("gateway.weight", "not-a-number"));

            assertEquals(ServiceInstanceMeta.DEFAULT_WEIGHT, meta.weight());
        }

        @Test
        @DisplayName("an out-of-range weight falls back rather than failing construction")
        void outOfRangeWeightFallsBack() {
            assertEquals(ServiceInstanceMeta.DEFAULT_WEIGHT,
                    ServiceInstanceMetaCodec.decode(Map.of("gateway.weight", "99999")).weight());
            assertEquals(ServiceInstanceMeta.DEFAULT_WEIGHT,
                    ServiceInstanceMetaCodec.decode(Map.of("gateway.weight", "0")).weight());
        }

        @Test
        @DisplayName("values violating the key's pattern are discarded, not propagated")
        void patternViolationsAreDiscarded() {
            ServiceInstanceMeta meta = ServiceInstanceMetaCodec.decode(
                    Map.of("gateway.zone", "bad zone!", "gateway.management-path", "no-leading-slash"));

            assertEquals("", meta.zone());
            assertEquals("", meta.managementPath());
        }

        @Test
        @DisplayName("an unparseable health timestamp becomes null")
        void malformedInstantBecomesNull() {
            assertNull(ServiceInstanceMetaCodec.decode(
                    Map.of("gateway.health-checked-at", "yesterday")).lastHealthCheckAt());
        }

        @Test
        @DisplayName("malformed tag entries are skipped without losing the good ones")
        void malformedTagsAreSkipped() {
            Map<String, String> tags = ServiceInstanceMetaCodec.decodeTags("good=1,broken,=2,also=ok");

            assertEquals(Map.of("good", "1", "also", "ok"), tags);
        }
    }

    @Nested
    @DisplayName("write-path validation is strict")
    class WriteValidationIsStrict {

        @Test
        @DisplayName("a malformed weight is rejected on the write path")
        void rejectsMalformedWeight() {
            assertThrows(IllegalArgumentException.class,
                    () -> ServiceInstanceMetaCodec.validate("gateway.weight", "abc"));
            assertThrows(IllegalArgumentException.class,
                    () -> ServiceInstanceMetaCodec.validate("gateway.weight", "0"));
        }

        @Test
        @DisplayName("unsorted tags are rejected so the wire form stays canonical")
        void rejectsUnsortedTags() {
            ServiceInstanceMetaCodec.validate("gateway.tags", "a=1,b=2");

            assertThrows(IllegalArgumentException.class,
                    () -> ServiceInstanceMetaCodec.validate("gateway.tags", "b=2,a=1"));
        }

        @Test
        @DisplayName("an unknown gateway key is accepted for forward compatibility")
        void acceptsUnknownReservedKeys() {
            // A newer provider may report a key this build predates; rejecting it would make
            // rolling upgrades order-dependent.
            ServiceInstanceMetaCodec.validate("gateway.some-future-key", "whatever");
        }

        @Test
        @DisplayName("an unrecognised health state is rejected, but UNKNOWN is accepted")
        void validatesHealthState() {
            ServiceInstanceMetaCodec.validate("gateway.health-state", "UNKNOWN");
            ServiceInstanceMetaCodec.validate("gateway.health-state", "OUT_OF_SERVICE");

            assertThrows(IllegalArgumentException.class,
                    () -> ServiceInstanceMetaCodec.validate("gateway.health-state", "SORT_OF_UP"));
        }
    }

    @Nested
    @DisplayName("merge and separation")
    class MergeAndSeparation {

        @Test
        @DisplayName("merge replaces stale reserved keys instead of leaving them behind")
        void mergeReplacesStaleReservedKeys() {
            // Regression guard: if weight reverts to its default, the old entry must go,
            // otherwise the instance keeps routing at the previous weight forever.
            Map<String, String> existing = new LinkedHashMap<>();
            existing.put("gateway.weight", "900");
            existing.put("team", "payments");

            Map<String, String> merged = ServiceInstanceMetaCodec.merge(
                    existing, ServiceInstanceMeta.defaults());

            assertFalse(merged.containsKey("gateway.weight"));
            assertEquals("payments", merged.get("team"));
        }

        @Test
        @DisplayName("business metadata is recoverable from a merged map")
        void businessMetadataExcludesReservedKeys() {
            Map<String, String> merged = ServiceInstanceMetaCodec.merge(
                    Map.of("team", "payments"), ServiceInstanceMeta.defaults().withWeight(500));

            assertEquals(Map.of("team", "payments"),
                    ServiceInstanceMetaCodec.businessMetadata(merged));
        }
    }

    @Nested
    @DisplayName("effective weight")
    class EffectiveWeight {

        private final Instant now = Instant.parse("2026-07-26T12:00:00Z");

        @Test
        @DisplayName("a healthy instance outside its warm-up window uses its full weight")
        void healthyUsesFullWeight() {
            ServiceInstanceMeta meta = ServiceInstanceMeta.defaults()
                    .withHealthState(InstanceHealthState.UP, now);

            assertEquals(100, meta.effectiveWeight(now, now.minusSeconds(600)));
        }

        @Test
        @DisplayName("a degraded instance is halved rather than removed")
        void degradedIsHalved() {
            ServiceInstanceMeta meta = ServiceInstanceMeta.defaults()
                    .withHealthState(InstanceHealthState.DEGRADED, now);

            assertEquals(50, meta.effectiveWeight(now, now.minusSeconds(600)));
        }

        @Test
        @DisplayName("down and drained instances reach zero so callers can detect an empty pool")
        void unselectableStatesAreZero() {
            assertEquals(0, ServiceInstanceMeta.defaults()
                    .withHealthState(InstanceHealthState.DOWN, now)
                    .effectiveWeight(now, now.minusSeconds(600)));
            assertEquals(0, ServiceInstanceMeta.defaults()
                    .withHealthState(InstanceHealthState.OUT_OF_SERVICE, now)
                    .effectiveWeight(now, now.minusSeconds(600)));
        }

        @Test
        @DisplayName("weight ramps linearly across the warm-up window")
        void warmupRampsLinearly() {
            ServiceInstanceMeta meta = new ServiceInstanceMeta(
                    100, "", "", Map.of(), "", "", "", "", "",
                    100, InstanceHealthState.UP, null);

            assertEquals(25, meta.effectiveWeight(now, now.minusSeconds(25)));
            assertEquals(50, meta.effectiveWeight(now, now.minusSeconds(50)));
            assertEquals(100, meta.effectiveWeight(now, now.minusSeconds(100)));
        }

        @Test
        @DisplayName("a warming instance never reaches zero, or it could never warm up")
        void warmingInstanceNeverStarves() {
            ServiceInstanceMeta meta = new ServiceInstanceMeta(
                    100, "", "", Map.of(), "", "", "", "", "",
                    3600, InstanceHealthState.UP, null);

            assertEquals(1, meta.effectiveWeight(now, now.minusSeconds(1)));
        }

        @Test
        @DisplayName("unknown health stays selectable so upgrades cannot drain legacy instances")
        void unknownIsSelectable() {
            assertTrue(InstanceHealthState.UNKNOWN.selectable());
            assertEquals(100, ServiceInstanceMeta.defaults().effectiveWeight(now, null));
        }
    }
}
