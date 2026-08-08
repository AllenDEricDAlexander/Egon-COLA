package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

/**
 * 配置发布操作的聚合结果。 / Aggregate result of a configuration publication operation.
 *
 * @param changeId        发布变更标识 / publication change identifier
 * @param status          聚合发布状态 / aggregate publication status
 * @param targetVersion   目标配置版本 / target configuration version
 * @param resourceChecksum 发布资源校验和 / checksum of the published resource
 * @param targetCount     发布目标总数 / total number of publication targets
 * @param targets         各实例的发布目标结果 / per-instance publication-target results
 * @param errorMessage    聚合错误消息，无错误时为空 / aggregate error message, null when no error occurred
 * @param createdAt       发布任务创建时间 / publication-task creation time
 * @param dispatchedAt    发布任务分发时间 / publication-task dispatch time
 * @param completedAt     发布任务完成时间 / publication-task completion time
 */
public record DdcManagementPublishResult(
        String changeId,
        DdcManagementPublishStatus status,
        Long targetVersion,
        String resourceChecksum,
        int targetCount,
        List<DdcManagementPublishTarget> targets,
        String errorMessage,
        Instant createdAt,
        Instant dispatchedAt,
        Instant completedAt
) {

    /**
     * 构造发布结果并将目标列表归一化为不可变列表。 /
     * Constructs a publication result and normalizes the target list to an immutable list.
     */
    public DdcManagementPublishResult {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }
}
