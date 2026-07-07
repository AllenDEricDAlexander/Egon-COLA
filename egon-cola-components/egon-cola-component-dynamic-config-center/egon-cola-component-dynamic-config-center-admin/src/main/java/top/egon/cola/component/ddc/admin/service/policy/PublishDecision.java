package top.egon.cola.component.ddc.admin.service.policy;

import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;

public record PublishDecision(boolean completed, PublishStatus status) {

    public static PublishDecision running() {
        return new PublishDecision(false, PublishStatus.PUBLISHING);
    }

    public static PublishDecision completed(PublishStatus status) {
        return new PublishDecision(true, status);
    }
}
