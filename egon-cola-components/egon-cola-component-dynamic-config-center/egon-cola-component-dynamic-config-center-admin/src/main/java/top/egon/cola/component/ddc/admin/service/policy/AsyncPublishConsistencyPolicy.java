package top.egon.cola.component.ddc.admin.service.policy;

import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;

public class AsyncPublishConsistencyPolicy implements PublishConsistencyPolicy {

    @Override
    public PublishDecision afterMessagePublished() {
        return PublishDecision.completed(PublishStatus.SUCCESS);
    }

    @Override
    public PublishDecision decide(int targetCount, int ackCount, int failedCount, int timeoutCount) {
        return PublishDecision.completed(PublishStatus.SUCCESS);
    }
}
