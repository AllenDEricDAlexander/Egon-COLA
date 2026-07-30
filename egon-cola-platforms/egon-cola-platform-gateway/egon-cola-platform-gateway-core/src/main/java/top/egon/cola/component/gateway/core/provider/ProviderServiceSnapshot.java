package top.egon.cola.component.gateway.core.provider;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ProviderServiceSnapshot(
        ProviderServiceKey serviceKey,
        long revision,
        Instant observedAt,
        List<ProviderInstance> instances
) {

    public ProviderServiceSnapshot {
        serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        ProviderServiceKey expectedServiceKey = serviceKey;
        instances = Objects.requireNonNull(instances, "instances")
                .stream()
                .peek(instance -> {
                    if (!expectedServiceKey.equals(instance.serviceKey())) {
                        throw new IllegalArgumentException(
                                "instance service key does not match snapshot"
                        );
                    }
                })
                .sorted(Comparator.comparing(ProviderInstance::runtimeIdentity))
                .toList();
    }
}
