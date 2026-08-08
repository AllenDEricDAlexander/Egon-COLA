package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.util.List;

/**
 * 可查询的配置发布任务详情。 / Queryable details of a configuration publication task.
 *
 * @param changeId        发布变更标识 / publication change identifier
 * @param status          聚合发布状态 / aggregate publication status
 * @param targetVersion   目标配置版本 / target configuration version
 * @param resourceChecksum 发布资源校验和 / checksum of the published content
 * @param targetCount     发布目标总数 / total number of publication targets
 * @param ackCount        已确认目标数 / number of acknowledged targets
 * @param failedCount     失败目标数 / number of failed targets
 * @param ignoredCount    已忽略目标数 / number of ignored targets
 * @param timeoutCount    超时目标数 / number of timed-out targets
 * @param attemptCount    发布尝试次数 / number of publication attempts
 * @param targets         各实例的发布目标结果 / per-instance publication-target results
 * @param errorMessage    聚合错误消息，无错误时为空 / aggregate error message, null when no error occurred
 * @param createdAt       发布任务创建时间 / publication-task creation time
 * @param dispatchedAt    最近一次分发时间 / most recent dispatch time
 * @param completedAt     发布任务完成时间 / publication-task completion time
 */
public record DdcManagementPublishTask(
        String changeId,
        DdcManagementPublishStatus status,
        Long targetVersion,
        String resourceChecksum,
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

    /**
     * 构造发布任务并将目标列表归一化为不可变列表。 /
     * Constructs a publication task and normalizes the target list to an immutable list.
     */
    public DdcManagementPublishTask {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }
}
