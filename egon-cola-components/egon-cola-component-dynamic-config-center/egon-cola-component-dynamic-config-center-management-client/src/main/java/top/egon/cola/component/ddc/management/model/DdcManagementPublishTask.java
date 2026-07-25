package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

public record DdcManagementPublishTask(
        String changeId,
        DdcManagementPublishStatus status,
        Long targetVersion,
        String contentChecksum,
        int targetCount,
        int ackCount,
        int failedCount,
        int ignoredCount,
        int timeoutCount,
        int attemptCount,
        List<DdcManagementPublishTarget> targets,
        String errorMessage,
        Instant createdAt,
        Instant dispatchedAt,
        Instant completedAt
) {

    public DdcManagementPublishTask {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }
}
