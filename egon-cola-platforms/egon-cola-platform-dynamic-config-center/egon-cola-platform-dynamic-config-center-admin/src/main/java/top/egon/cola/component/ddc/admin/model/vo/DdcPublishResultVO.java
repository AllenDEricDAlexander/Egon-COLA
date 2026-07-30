package top.egon.cola.component.ddc.admin.model.vo;

import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;

@Getter
@Setter
public class DdcPublishResultVO {

    private String changeId;

    private String status;

    private Integer targetCount;

    private Integer ackCount;

    private Integer failedCount;

    private Integer ignoredCount;

    private Integer timeoutCount;

    private Integer attemptCount;

    private Long targetVersion;

    private String contentChecksum;

    private String errorMessage;

    public static DdcPublishResultVO from(DdcPublishTaskEntity task) {
        DdcPublishResultVO vo = new DdcPublishResultVO();
        vo.setChangeId(task.getChangeId());
        vo.setStatus(task.getStatus());
        vo.setTargetCount(task.getTargetCount());
        vo.setAckCount(task.getAckCount());
        vo.setFailedCount(task.getFailedCount());
        vo.setIgnoredCount(task.getIgnoredCount());
        vo.setTimeoutCount(task.getTimeoutCount());
        vo.setAttemptCount(task.getAttemptCount());
        vo.setTargetVersion(task.getTargetVersion());
        vo.setContentChecksum(task.getContentChecksum());
        vo.setErrorMessage(task.getErrorMessage());
        return vo;
    }
}
