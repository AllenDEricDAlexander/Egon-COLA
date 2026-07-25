package top.egon.cola.component.rpc.provider;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class RpcProviderMetadataMerger {

    private static final Pattern LOCATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}");

    private static final Pattern TAG_KEY =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,31}");

    private static final Pattern TAG_VALUE =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/+-]{0,63}");

    private static final Pattern MANAGEMENT_PATH =
            Pattern.compile("/[A-Za-z0-9/_.{}-]{0,255}");

    private final List<RpcProviderMetadataContributor> contributors;

    public RpcProviderMetadataMerger(
            Iterable<RpcProviderMetadataContributor> contributors
    ) {
        List<RpcProviderMetadataContributor> ordered = new ArrayList<>();
        if (contributors != null) {
            contributors.forEach(ordered::add);
        }
        AnnotationAwareOrderComparator.sort(ordered);
        this.contributors = List.copyOf(ordered);
    }

    public Map<String, String> merge(
            RpcServiceIdentity serviceIdentity,
            Map<String, String> configuredMetadata
    ) {
        if (serviceIdentity == null) {
            throw new IllegalArgumentException(
                    "RPC service identity is required"
            );
        }
        Map<String, String> merged = new TreeMap<>();
        mergeSource(merged, configuredMetadata);
        contributors.forEach(contributor ->
                mergeSource(merged, contributor.contribute(serviceIdentity)));
        validateGatewayMetadata(merged);
        return Collections.unmodifiableMap(merged);
    }

    private void mergeSource(
            Map<String, String> merged,
            Map<String, String> source
    ) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            validateCustomKey(key);
            String normalizedValue = value == null ? "" : value;
            String existing = merged.putIfAbsent(key, normalizedValue);
            if (existing != null && !existing.equals(normalizedValue)) {
                throw new IllegalArgumentException(
                        "RPC Provider metadata key conflict: " + key
                );
            }
        });
    }

    private void validateCustomKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC Provider metadata key is required"
            );
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ddc.")
                || lower.startsWith("egon.internal.")
                || lower.startsWith("egon.rpc.")) {
            throw new IllegalArgumentException(
                    "RPC Provider metadata uses a reserved key"
            );
        }
    }

    private void validateGatewayMetadata(Map<String, String> metadata) {
        metadata.forEach((key, value) -> {
            switch (key) {
                case "gateway.weight" -> validateWeight(value);
                case "gateway.tags" -> validateTags(value);
                case "gateway.zone", "gateway.region" ->
                        requirePattern(key, value, LOCATION);
                case "gateway.protocol-version",
                     "gateway.definition-set-id",
                     "gateway.artifact-version",
                     "gateway.build-id" ->
                        requirePattern(key, value, IDENTIFIER);
                case "gateway.management-path" ->
                        requirePattern(key, value, MANAGEMENT_PATH);
                default -> {
                    // Other extension keys remain governed by DDC capacity and
                    // secret guards when the registration is constructed.
                }
            }
        });
    }

    private void validateWeight(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw invalid("gateway.weight");
        }
        try {
            int weight = Integer.parseInt(value);
            if (weight < 1 || weight > 10000) {
                throw invalid("gateway.weight");
            }
        } catch (NumberFormatException exception) {
            throw invalid("gateway.weight");
        }
    }

    private void validateTags(String value) {
        if (value == null || value.isBlank() || value.length() > 256) {
            throw invalid("gateway.tags");
        }
        String[] entries = value.split(",", -1);
        if (entries.length > 32) {
            throw invalid("gateway.tags");
        }
        Set<String> keys = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String entry : entries) {
            String[] pair = entry.split("=", -1);
            if (pair.length != 2
                    || !TAG_KEY.matcher(pair[0]).matches()
                    || !TAG_VALUE.matcher(pair[1]).matches()
                    || !keys.add(pair[0])) {
                throw invalid("gateway.tags");
            }
            normalized.add(entry);
        }
        List<String> sorted = normalized.stream().sorted().toList();
        if (!normalized.equals(sorted)) {
            throw invalid("gateway.tags");
        }
    }

    private void requirePattern(
            String key,
            String value,
            Pattern pattern
    ) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw invalid(key);
        }
    }

    private IllegalArgumentException invalid(String key) {
        return new IllegalArgumentException(
                "RPC Provider metadata value is invalid for " + key
        );
    }
}
