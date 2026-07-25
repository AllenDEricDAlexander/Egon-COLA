package top.egon.cola.component.ddc.management.model;

import java.time.Instant;

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
        Instant expireAt
) {
}
