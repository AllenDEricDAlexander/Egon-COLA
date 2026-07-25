package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Map;

public record DdcManagementConfigClientInstance(
        String appCode,
        String env,
        String namespace,
        String instanceId,
        String leaseId,
        String host,
        Integer port,
        String leaseRole,
        String status,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant expireAt,
        Map<String, String> metadata
) {

    public DdcManagementConfigClientInstance {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
