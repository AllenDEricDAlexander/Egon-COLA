package top.egon.cola.component.ddc.management.model;

import java.time.Instant;

public record DdcManagementPublishTarget(
        String instanceId,
        String leaseId,
        Long currentVersion,
        String status,
        String errorMessage,
        Instant ackAt
) {
}
