package top.egon.cola.component.ddc.management.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Typed view over the {@code gateway.*} instance metadata that routing decisions depend on.
 *
 * <p>These keys are not new. Providers have been writing {@code gateway.weight},
 * {@code gateway.zone}, {@code gateway.tags} and friends for some time, but the convention
 * lived as duplicated string literals — written and validated in the RPC provider's metadata
 * merger, read again in the gateway's provider instance model, with no shared definition and
 * no agreement on what a malformed value means. This record and
 * {@link ServiceInstanceMetaCodec} make that convention a single typed thing.
 *
 * <p>The registry model is untouched: no field is added to any registry record, and the wire
 * format is still a flat {@code Map<String, String>}. A provider that writes none of these keys
 * decodes to exactly the defaults below and behaves as it did before.
 *
 * <p>Only {@link #warmupSeconds()}, {@link #healthState()} and {@link #lastHealthCheckAt()} are
 * genuinely new keys; every other field maps to a key that already existed.
 *
 * @param weight            relative load-balancing weight, {@value #MIN_WEIGHT}..{@value #MAX_WEIGHT}
 * @param region            geographic region, empty when unspecified
 * @param zone              availability zone, empty when unspecified
 * @param tags              key/value labels used for canary and grey-release matching
 * @param protocolVersion   concrete wire version, e.g. {@code HTTP/1.1}, {@code h2}, {@code grpc}
 * @param definitionSetId   fingerprint of the interface definition set this instance serves
 * @param artifactVersion   build artifact version
 * @param buildId           build identifier
 * @param managementPath    actuator/management base path used by active health probes
 * @param warmupSeconds     <em>new key.</em> ramp-up window over which effective weight scales
 *                          linearly from zero, so a freshly started instance is not saturated
 * @param healthState       <em>new key.</em> last observed health
 * @param lastHealthCheckAt <em>new key.</em> timestamp of the last probe, null when never probed
 */
public record ServiceInstanceMeta(
        int weight,
        String region,
        String zone,
        Map<String, String> tags,
        String protocolVersion,
        String definitionSetId,
        String artifactVersion,
        String buildId,
        String managementPath,
        int warmupSeconds,
        InstanceHealthState healthState,
        Instant lastHealthCheckAt
) {

    public static final int DEFAULT_WEIGHT = 100;
    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 10_000;
    public static final int MAX_WARMUP_SECONDS = 3600;
    /** Matches the tag-count bound the RPC provider metadata merger already enforces. */
    public static final int MAX_TAGS = 32;

    private static final ServiceInstanceMeta DEFAULTS = new ServiceInstanceMeta(
            DEFAULT_WEIGHT, "", "", Map.of(), "", "", "", "", "", 0,
            InstanceHealthState.UNKNOWN, null);

    public ServiceInstanceMeta {
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new IllegalArgumentException(
                    "weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT + " but was " + weight);
        }
        if (warmupSeconds < 0 || warmupSeconds > MAX_WARMUP_SECONDS) {
            throw new IllegalArgumentException(
                    "warmupSeconds must be between 0 and " + MAX_WARMUP_SECONDS + " but was " + warmupSeconds);
        }
        region = normalize(region);
        zone = normalize(zone);
        protocolVersion = normalize(protocolVersion);
        definitionSetId = normalize(definitionSetId);
        artifactVersion = normalize(artifactVersion);
        buildId = normalize(buildId);
        managementPath = normalize(managementPath);
        tags = normalizeTags(tags);
        healthState = healthState == null ? InstanceHealthState.UNKNOWN : healthState;
    }

    /** The all-defaults instance: weight 100, no placement, health {@code UNKNOWN}. */
    public static ServiceInstanceMeta defaults() {
        return DEFAULTS;
    }

    /**
     * Weight to use for this instance right now, folding in health and warm-up.
     *
     * <p>Returns 0 for instances that must not be selected, so a caller can sum effective
     * weights and detect "nothing is routable" in a single pass.
     *
     * @param now          current time
     * @param registeredAt when the instance joined; null disables warm-up scaling
     */
    public int effectiveWeight(Instant now, Instant registeredAt) {
        int base = weight * healthState.weightPercent() / 100;
        if (base <= 0) {
            return 0;
        }
        int warmed = applyWarmup(base, now, registeredAt);
        // A selectable instance never drops to zero, otherwise it could never warm up at all.
        return Math.max(warmed, 1);
    }

    private int applyWarmup(int base, Instant now, Instant registeredAt) {
        if (warmupSeconds <= 0 || registeredAt == null || now == null) {
            return base;
        }
        long elapsed = Duration.between(registeredAt, now).getSeconds();
        if (elapsed >= warmupSeconds) {
            return base;
        }
        if (elapsed <= 0) {
            return 0;
        }
        return (int) (base * elapsed / warmupSeconds);
    }

    /** Whether this instance sits in the given zone; blank zones never match. */
    public boolean inZone(String candidateZone) {
        return !zone.isEmpty() && zone.equalsIgnoreCase(candidateZone);
    }

    /** Whether this instance carries {@code tag=value}; used for canary matching. */
    public boolean hasTag(String tag, String value) {
        if (tag == null || value == null) {
            return false;
        }
        return value.equals(tags.get(tag.toLowerCase(Locale.ROOT)));
    }

    public ServiceInstanceMeta withHealthState(InstanceHealthState state, Instant checkedAt) {
        return new ServiceInstanceMeta(weight, region, zone, tags, protocolVersion, definitionSetId,
                artifactVersion, buildId, managementPath, warmupSeconds, state, checkedAt);
    }

    public ServiceInstanceMeta withWeight(int newWeight) {
        return new ServiceInstanceMeta(newWeight, region, zone, tags, protocolVersion, definitionSetId,
                artifactVersion, buildId, managementPath, warmupSeconds, healthState, lastHealthCheckAt);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<String, String> normalizeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        // Sorted: the wire form must be deterministic, and the provider-side validator
        // rejects tag strings whose entries are not in ascending order.
        TreeMap<String, String> copy = new TreeMap<>();
        tags.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            copy.put(key.trim().toLowerCase(Locale.ROOT), value.trim());
        });
        if (copy.size() > MAX_TAGS) {
            throw new IllegalArgumentException("tags must contain at most " + MAX_TAGS + " entries");
        }
        return Collections.unmodifiableMap(copy);
    }
}
