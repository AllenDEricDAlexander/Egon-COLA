package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

public record DdcManagementServiceCatalog(
        long generation,
        Instant observedAt,
        List<DdcManagementServiceKey> services
) {

    public DdcManagementServiceCatalog {
        services = services == null ? List.of() : List.copyOf(services);
    }
}
