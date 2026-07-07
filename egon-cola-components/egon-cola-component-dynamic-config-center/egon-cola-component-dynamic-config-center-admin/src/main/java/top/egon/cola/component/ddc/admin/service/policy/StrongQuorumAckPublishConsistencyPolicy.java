package top.egon.cola.component.ddc.admin.service.policy;

import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;

public class StrongQuorumAckPublishConsistencyPolicy implements PublishConsistencyPolicy {

    @Override
    public PublishDecision afterMessagePublished() {
        return PublishDecision.running();
    }

    @Override
    public PublishDecision decide(int targetCount, int ackCount, int failedCount, int timeoutCount) {
        if (targetCount <= 0) {
            return PublishDecision.running();
        }
        int majority = targetCount / 2 + 1;
        if (ackCount >= majority) {
            return PublishDecision.completed(PublishStatus.SUCCESS);
        }
        if (failedCount + timeoutCount > targetCount - majority) {
            return PublishDecision.completed(PublishStatus.FAILED);
        }
        return PublishDecision.running();
    }
}
