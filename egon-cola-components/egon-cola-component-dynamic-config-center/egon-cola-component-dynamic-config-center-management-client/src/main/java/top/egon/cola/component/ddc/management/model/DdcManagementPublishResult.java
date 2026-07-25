package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

public record DdcManagementPublishResult(
        String changeId,
        DdcManagementPublishStatus status,
        Long targetVersion,
        String contentChecksum,
        int targetCount,
        List<DdcManagementPublishTarget> targets,
        String errorMessage,
        Instant createdAt,
        Instant dispatchedAt,
        Instant completedAt
) {

    public DdcManagementPublishResult {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }
}
