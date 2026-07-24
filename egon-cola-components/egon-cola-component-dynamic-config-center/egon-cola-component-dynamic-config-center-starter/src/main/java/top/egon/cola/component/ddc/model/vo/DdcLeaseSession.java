package top.egon.cola.component.ddc.model.vo;

import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;

import java.time.Instant;

public record DdcLeaseSession(
        String instanceId,
        String leaseId,
        DdcLeaseRole role,
        int leaseSeconds,
        int heartbeatIntervalSeconds,
        Instant registeredAt,
        Instant leaseExpireAt
) {
}
