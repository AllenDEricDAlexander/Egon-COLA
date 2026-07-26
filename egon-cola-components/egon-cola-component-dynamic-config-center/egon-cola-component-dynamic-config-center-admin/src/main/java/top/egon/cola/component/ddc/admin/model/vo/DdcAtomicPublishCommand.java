package top.egon.cola.component.ddc.admin.model.vo;

import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

public record DdcAtomicPublishCommand(
        String configId,
        String changeId,
        String appCode,
        String env,
        String namespace,
        String configKey,
        Long expectedPublishedVersion,
        Long targetVersion,
        String content,
        String eventChecksum,
        DdcPublishMessage message
) {
}
