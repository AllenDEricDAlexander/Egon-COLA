package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

public record DdcManagementServiceSnapshot(
        DdcManagementServiceKey serviceKey,
        long generation,
        Instant observedAt,
        List<DdcManagementServiceInstance> instances
) {

    public DdcManagementServiceSnapshot {
        instances = instances == null ? List.of() : List.copyOf(instances);
    }
}
