package top.egon.cola.component.ddc.model.registry;

import java.time.Instant;
import java.util.List;

public record DdcServiceSnapshot(
        DdcServiceKey serviceKey,
        long revision,
        List<DdcServiceInstance> instances,
        Instant observedAt
) {

    public DdcServiceSnapshot {
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        instances = instances == null
                ? List.of()
                : instances.stream().sorted().toList();
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
