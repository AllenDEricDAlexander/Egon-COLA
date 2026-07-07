package top.egon.cola.component.ddc.admin.service.policy;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PublishConsistencyPolicyTest {

    @Test
    void allAckRequiresEveryTargetSuccess() {
        PublishConsistencyPolicy policy = new StrongAllAckPublishConsistencyPolicy();

        PublishDecision decision = policy.decide(3, 3, 0, 0);

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }

    @Test
    void quorumAckCompletesWhenMajoritySucceeded() {
        PublishConsistencyPolicy policy = new StrongQuorumAckPublishConsistencyPolicy();

        PublishDecision decision = policy.decide(5, 3, 1, 0);

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }

    @Test
    void asyncCompletesAfterMessagePublish() {
        PublishConsistencyPolicy policy = new AsyncPublishConsistencyPolicy();

        PublishDecision decision = policy.afterMessagePublished();

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }
}
