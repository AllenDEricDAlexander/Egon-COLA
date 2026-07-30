package top.egon.cola.component.gateway.core.provider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProviderCatalogSnapshot(
        long revision,
        Instant observedAt,
        List<ProviderServiceKey> serviceKeys
) {

    public ProviderCatalogSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        serviceKeys = Objects.requireNonNull(serviceKeys, "serviceKeys")
                .stream()
                .sorted()
                .distinct()
                .toList();
    }
}
