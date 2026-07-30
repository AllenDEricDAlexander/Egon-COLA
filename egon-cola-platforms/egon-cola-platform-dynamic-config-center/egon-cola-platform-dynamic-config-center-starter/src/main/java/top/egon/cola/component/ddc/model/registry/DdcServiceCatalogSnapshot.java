package top.egon.cola.component.ddc.model.registry;

import java.time.Instant;
import java.util.List;

public record DdcServiceCatalogSnapshot(
        DdcServiceQuery query,
        long revision,
        List<DdcServiceKey> serviceKeys,
        Instant observedAt
) {

    public DdcServiceCatalogSnapshot {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        serviceKeys = serviceKeys == null
                ? List.of()
                : serviceKeys.stream().sorted().toList();
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
