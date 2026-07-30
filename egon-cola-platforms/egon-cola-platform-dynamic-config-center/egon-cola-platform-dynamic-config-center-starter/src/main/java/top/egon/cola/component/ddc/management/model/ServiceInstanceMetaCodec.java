package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Canonical definition of the {@code gateway.*} instance-metadata convention, and the
 * bidirectional projection between it and {@link ServiceInstanceMeta}.
 *
 * <p>Before this class the convention existed only as duplicated string literals: the RPC
 * provider metadata merger validated {@code gateway.weight} on the way out, and the gateway's
 * provider instance model parsed it again on the way in, with no shared constant and two
 * different opinions about malformed input. Both sides now delegate here.
 *
 * <p><strong>Encoding omits defaults.</strong> A field equal to its default contributes no
 * entry, so an all-default instance registers byte-identically to one registered before this
 * type existed, and the metadata budget stays free for business keys.
 *
 * <p><strong>Decoding never throws.</strong> Decode runs on the routing hot path against data
 * written by another process, possibly an older or newer one. A malformed value falls back to
 * its default rather than propagating an exception that would drop a service out of discovery.
 * Writes are validated ({@link ServiceInstanceMeta}'s constructor and {@link #validate}); reads
 * are tolerant. That asymmetry is deliberate.
 */
public final class ServiceInstanceMetaCodec {

    /** Reserved namespace for structured instance metadata. */
    public static final String PREFIX = "gateway.";

    public static final String KEY_WEIGHT = PREFIX + "weight";
    public static final String KEY_REGION = PREFIX + "region";
    public static final String KEY_ZONE = PREFIX + "zone";
    public static final String KEY_TAGS = PREFIX + "tags";
    public static final String KEY_PROTOCOL_VERSION = PREFIX + "protocol-version";
    public static final String KEY_DEFINITION_SET_ID = PREFIX + "definition-set-id";
    public static final String KEY_ARTIFACT_VERSION = PREFIX + "artifact-version";
    public static final String KEY_BUILD_ID = PREFIX + "build-id";
    public static final String KEY_MANAGEMENT_PATH = PREFIX + "management-path";

    /** New in this revision: warm-up ramp window. */
    public static final String KEY_WARMUP_SECONDS = PREFIX + "warmup-seconds";
    /** New in this revision: last observed health. */
    public static final String KEY_HEALTH_STATE = PREFIX + "health-state";
    /** New in this revision: timestamp of the last health probe. */
    public static final String KEY_HEALTH_CHECKED_AT = PREFIX + "health-checked-at";

    /** Upper bound on how many reserved keys a single instance can carry. */
    public static final int RESERVED_KEY_COUNT = 12;

    /** Region and zone: short placement identifiers. */
    public static final Pattern LOCATION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    /** Version and identifier valued keys. */
    public static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}");
    public static final Pattern TAG_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,31}");
    public static final Pattern TAG_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/+-]{0,63}");
    public static final Pattern MANAGEMENT_PATH = Pattern.compile("/[A-Za-z0-9/_.{}-]{0,255}");

    private static final int MAX_TAGS_LENGTH = 256;
    private static final String TAG_SEPARATOR = ",";
    private static final String TAG_ASSIGNMENT = "=";

    private ServiceInstanceMetaCodec() {
    }

    /** Whether a metadata key belongs to the reserved structured namespace. */
    public static boolean isReservedKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).startsWith(PREFIX);
    }

    /**
     * Renders the non-default fields of {@code meta} as metadata entries.
     *
     * @return a mutable map containing only reserved keys; empty when {@code meta} is all defaults
     */
    public static Map<String, String> encode(ServiceInstanceMeta meta) {
        Map<String, String> encoded = new LinkedHashMap<>();
        if (meta == null) {
            return encoded;
        }
        ServiceInstanceMeta defaults = ServiceInstanceMeta.defaults();
        if (meta.weight() != defaults.weight()) {
            encoded.put(KEY_WEIGHT, Integer.toString(meta.weight()));
        }
        putIfPresent(encoded, KEY_REGION, meta.region());
        putIfPresent(encoded, KEY_ZONE, meta.zone());
        if (!meta.tags().isEmpty()) {
            encoded.put(KEY_TAGS, encodeTags(meta.tags()));
        }
        putIfPresent(encoded, KEY_PROTOCOL_VERSION, meta.protocolVersion());
        putIfPresent(encoded, KEY_DEFINITION_SET_ID, meta.definitionSetId());
        putIfPresent(encoded, KEY_ARTIFACT_VERSION, meta.artifactVersion());
        putIfPresent(encoded, KEY_BUILD_ID, meta.buildId());
        putIfPresent(encoded, KEY_MANAGEMENT_PATH, meta.managementPath());
        if (meta.warmupSeconds() != defaults.warmupSeconds()) {
            encoded.put(KEY_WARMUP_SECONDS, Integer.toString(meta.warmupSeconds()));
        }
        if (meta.healthState() != defaults.healthState()) {
            encoded.put(KEY_HEALTH_STATE, meta.healthState().name());
        }
        if (meta.lastHealthCheckAt() != null) {
            encoded.put(KEY_HEALTH_CHECKED_AT, meta.lastHealthCheckAt().toString());
        }
        return encoded;
    }

    /**
     * Reads the reserved keys out of {@code metadata}, falling back to defaults field by field.
     *
     * <p>Unrecognised or malformed values are ignored rather than rejected. See the class note.
     */
    public static ServiceInstanceMeta decode(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return ServiceInstanceMeta.defaults();
        }
        ServiceInstanceMeta defaults = ServiceInstanceMeta.defaults();
        int weight = clamp(readInt(metadata, KEY_WEIGHT, defaults.weight()),
                ServiceInstanceMeta.MIN_WEIGHT, ServiceInstanceMeta.MAX_WEIGHT, defaults.weight());
        int warmupSeconds = clamp(readInt(metadata, KEY_WARMUP_SECONDS, defaults.warmupSeconds()),
                0, ServiceInstanceMeta.MAX_WARMUP_SECONDS, defaults.warmupSeconds());
        return new ServiceInstanceMeta(
                weight,
                readMatching(metadata, KEY_REGION, LOCATION),
                readMatching(metadata, KEY_ZONE, LOCATION),
                decodeTags(metadata.get(KEY_TAGS)),
                readMatching(metadata, KEY_PROTOCOL_VERSION, IDENTIFIER),
                readMatching(metadata, KEY_DEFINITION_SET_ID, IDENTIFIER),
                readMatching(metadata, KEY_ARTIFACT_VERSION, IDENTIFIER),
                readMatching(metadata, KEY_BUILD_ID, IDENTIFIER),
                readMatching(metadata, KEY_MANAGEMENT_PATH, MANAGEMENT_PATH),
                warmupSeconds,
                InstanceHealthState.fromWire(rawString(metadata, KEY_HEALTH_STATE)),
                readInstant(metadata, KEY_HEALTH_CHECKED_AT));
    }

    /**
     * Merges the encoded form of {@code meta} into {@code businessMetadata}.
     *
     * <p>Reserved keys already present are dropped first, so a re-registration replaces
     * structured metadata wholesale instead of leaving a stale field behind when a value
     * reverts to its default.
     *
     * @return a new map; neither argument is modified
     */
    public static Map<String, String> merge(Map<String, String> businessMetadata, ServiceInstanceMeta meta) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (businessMetadata != null) {
            businessMetadata.forEach((key, value) -> {
                if (!isReservedKey(key)) {
                    merged.put(key, value);
                }
            });
        }
        merged.putAll(encode(meta));
        return merged;
    }

    /** Returns only the caller's own metadata, with structured entries removed. */
    public static Map<String, String> businessMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(entry -> !isReservedKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> right, LinkedHashMap::new));
    }

    /**
     * Strictly validates one reserved metadata entry, for use on the write path.
     *
     * <p>Unknown {@code gateway.*} keys are accepted: this convention has to tolerate a newer
     * provider reporting a key this version does not know about.
     *
     * @throws IllegalArgumentException if the value is invalid for a known reserved key
     */
    public static void validate(String key, String value) {
        if (key == null) {
            return;
        }
        switch (key.toLowerCase(Locale.ROOT)) {
            case KEY_WEIGHT -> requireIntInRange(key, value,
                    ServiceInstanceMeta.MIN_WEIGHT, ServiceInstanceMeta.MAX_WEIGHT);
            case KEY_WARMUP_SECONDS -> requireIntInRange(key, value,
                    0, ServiceInstanceMeta.MAX_WARMUP_SECONDS);
            case KEY_TAGS -> requireValidTags(value);
            case KEY_ZONE, KEY_REGION -> requirePattern(key, value, LOCATION);
            case KEY_PROTOCOL_VERSION, KEY_DEFINITION_SET_ID, KEY_ARTIFACT_VERSION, KEY_BUILD_ID ->
                    requirePattern(key, value, IDENTIFIER);
            case KEY_MANAGEMENT_PATH -> requirePattern(key, value, MANAGEMENT_PATH);
            case KEY_HEALTH_STATE -> requireHealthState(value);
            case KEY_HEALTH_CHECKED_AT -> requireInstant(value);
            default -> {
                // Forward compatibility: a newer provider may report keys this build predates.
            }
        }
    }

    /** Applies {@link #validate} to every reserved entry in {@code metadata}. */
    public static void validateAll(Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        metadata.forEach((key, value) -> {
            if (isReservedKey(key)) {
                validate(key, value);
            }
        });
    }

    /** Encodes tags to the sorted {@code k=v,k=v} wire form. */
    public static String encodeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return new TreeMap<>(tags).entrySet().stream()
                .map(entry -> entry.getKey() + TAG_ASSIGNMENT + entry.getValue())
                .collect(Collectors.joining(TAG_SEPARATOR));
    }

    /** Decodes the {@code k=v,k=v} wire form, skipping malformed entries. */
    public static Map<String, String> decodeTags(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        TreeMap<String, String> tags = new TreeMap<>();
        for (String entry : value.split(TAG_SEPARATOR, -1)) {
            int split = entry.indexOf(TAG_ASSIGNMENT);
            if (split <= 0 || split == entry.length() - 1) {
                continue;
            }
            String tagKey = entry.substring(0, split).trim().toLowerCase(Locale.ROOT);
            String tagValue = entry.substring(split + 1).trim();
            if (tagKey.isEmpty() || tagValue.isEmpty() || tags.size() >= ServiceInstanceMeta.MAX_TAGS) {
                continue;
            }
            tags.put(tagKey, tagValue);
        }
        return Map.copyOf(tags);
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    private static String rawString(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value == null ? "" : value.trim();
    }

    /** Reads a string key, discarding values that do not match the convention's pattern. */
    private static String readMatching(Map<String, String> metadata, String key, Pattern pattern) {
        String value = rawString(metadata, key);
        if (value.isEmpty() || !pattern.matcher(value).matches()) {
            return "";
        }
        return value;
    }

    private static int readInt(Map<String, String> metadata, String key, int fallback) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    private static Instant readInstant(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static void requireIntInRange(String key, String value, int min, int max) {
        if (value == null || !value.matches("[0-9]+")) {
            throw invalid(key);
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw invalid(key);
            }
        } catch (NumberFormatException ex) {
            throw invalid(key);
        }
    }

    private static void requireValidTags(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_TAGS_LENGTH) {
            throw invalid(KEY_TAGS);
        }
        String[] entries = value.split(TAG_SEPARATOR, -1);
        if (entries.length > ServiceInstanceMeta.MAX_TAGS) {
            throw invalid(KEY_TAGS);
        }
        TreeMap<String, String> seen = new TreeMap<>();
        for (String entry : entries) {
            String[] pair = entry.split(TAG_ASSIGNMENT, -1);
            if (pair.length != 2
                    || !TAG_KEY.matcher(pair[0]).matches()
                    || !TAG_VALUE.matcher(pair[1]).matches()
                    || seen.put(pair[0], pair[1]) != null) {
                throw invalid(KEY_TAGS);
            }
        }
        // The wire form must be canonical so two equivalent tag sets hash identically.
        if (!encodeTags(seen).equals(value)) {
            throw invalid(KEY_TAGS);
        }
    }

    private static void requireHealthState(String value) {
        if (!InstanceHealthState.isKnownWireValue(value)) {
            throw invalid(KEY_HEALTH_STATE);
        }
    }

    private static void requireInstant(String value) {
        if (value == null) {
            throw invalid(KEY_HEALTH_CHECKED_AT);
        }
        try {
            Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw invalid(KEY_HEALTH_CHECKED_AT);
        }
    }

    private static void requirePattern(String key, String value, Pattern pattern) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw invalid(key);
        }
    }

    private static IllegalArgumentException invalid(String key) {
        return new IllegalArgumentException("instance metadata value is invalid for " + key);
    }
}
