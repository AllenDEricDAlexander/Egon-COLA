package top.egon.cola.component.ddc.admin.service.policy;

import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;

@Component
public class PublishConsistencyPolicyFactory {

    private final PublishConsistencyPolicy async = new AsyncPublishConsistencyPolicy();

    private final PublishConsistencyPolicy strongAllAck = new StrongAllAckPublishConsistencyPolicy();

    private final PublishConsistencyPolicy strongQuorumAck = new StrongQuorumAckPublishConsistencyPolicy();

    public PublishConsistencyPolicy get(PublishMode publishMode) {
        if (publishMode == PublishMode.STRONG_ALL_ACK) {
            return strongAllAck;
        }
        if (publishMode == PublishMode.STRONG_QUORUM_ACK) {
            return strongQuorumAck;
        }
        return async;
    }
}
