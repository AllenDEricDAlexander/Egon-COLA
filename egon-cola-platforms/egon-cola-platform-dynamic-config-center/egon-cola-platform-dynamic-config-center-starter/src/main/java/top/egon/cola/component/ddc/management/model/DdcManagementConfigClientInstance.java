package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.Map;

public record DdcManagementConfigClientInstance(
        String bizCode,
        String env,
        String appCode,
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

    public DdcInstanceStatus normalizedStatus() {
        return DdcInstanceStatus.fromWire(status);
    }
}
