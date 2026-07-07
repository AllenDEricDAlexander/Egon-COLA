package top.egon.cola.component.ddc.admin.service.policy;

public interface PublishConsistencyPolicy {

    PublishDecision afterMessagePublished();

    PublishDecision decide(int targetCount, int ackCount, int failedCount, int timeoutCount);
}
