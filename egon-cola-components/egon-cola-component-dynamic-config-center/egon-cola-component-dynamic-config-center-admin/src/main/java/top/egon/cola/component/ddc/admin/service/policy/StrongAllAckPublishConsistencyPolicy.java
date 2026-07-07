package top.egon.cola.component.ddc.admin.service.policy;

import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;

public class StrongAllAckPublishConsistencyPolicy implements PublishConsistencyPolicy {

    @Override
    public PublishDecision afterMessagePublished() {
        return PublishDecision.running();
    }

    @Override
    public PublishDecision decide(int targetCount, int ackCount, int failedCount, int timeoutCount) {
        if (targetCount <= 0) {
            return PublishDecision.running();
        }
        if (ackCount == targetCount) {
            return PublishDecision.completed(PublishStatus.SUCCESS);
        }
        if (ackCount + failedCount + timeoutCount >= targetCount && failedCount + timeoutCount > 0) {
            return PublishDecision.completed(PublishStatus.FAILED);
        }
        return PublishDecision.running();
    }
}
