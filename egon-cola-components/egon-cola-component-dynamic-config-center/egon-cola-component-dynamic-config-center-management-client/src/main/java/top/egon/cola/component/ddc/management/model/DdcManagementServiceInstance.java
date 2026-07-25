package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Map;

public record DdcManagementServiceInstance(
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant expireAt
) {

    public DdcManagementServiceInstance {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
