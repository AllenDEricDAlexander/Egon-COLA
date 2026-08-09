package top.egon.cola.component.rpc.provider;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Merges provider metadata from configuration and contributors into the map that is published
 * with the registration.
 *
 * <p>Validation of the {@code gateway.*} keys is delegated to {@link ServiceInstanceMetaCodec},
 * which is the single definition of that convention. It used to be re-implemented here with a
 * private copy of every pattern, while the gateway parsed the same keys back with a third set
 * of rules.
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
        ServiceInstanceMetaCodec.validateAll(merged);
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
}
