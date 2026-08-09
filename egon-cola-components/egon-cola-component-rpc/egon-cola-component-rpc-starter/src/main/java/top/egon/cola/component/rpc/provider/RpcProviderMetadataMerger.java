package top.egon.cola.component.rpc.provider;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Merges provider metadata without imposing registry-specific conventions.
 */
public final class RpcProviderMetadataMerger {

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
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "RPC Provider metadata value must not be blank"
                );
            }
            String normalizedValue = value.trim();
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
                    "RPC Provider metadata key must not be blank"
            );
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("egon.rpc.")) {
            throw new IllegalArgumentException(
                    "RPC Provider metadata uses a reserved key"
            );
        }
    }
}
