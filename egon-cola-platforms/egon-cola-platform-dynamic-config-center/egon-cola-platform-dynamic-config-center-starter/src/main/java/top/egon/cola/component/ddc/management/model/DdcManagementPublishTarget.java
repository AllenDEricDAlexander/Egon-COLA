package top.egon.cola.component.ddc.management.model;

import java.time.Instant;

/**
 * 单个配置客户端实例的发布目标结果。 / Publication-target result for one configuration-client instance.
 *
 * @param instanceId     目标实例标识 / target instance identifier
 * @param leaseId        目标实例租约标识 / target instance lease identifier
 * @param currentVersion 实例当前确认的配置版本 / configuration version currently acknowledged by the instance
 * @param status         目标发布状态 / target publication status
 * @param errorMessage   目标失败消息，无失败时为空 / target failure message, null when no failure occurred
 * @param ackAt          实例确认时间 / instance acknowledgement time
 */
public record DdcManagementPublishTarget(
        String instanceId,
        String leaseId,
        Long currentVersion,
        String status,
        String errorMessage,
        Instant ackAt
) {
}
