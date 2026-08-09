package top.egon.cola.component.ddc.admin.model.vo;

import top.egon.cola.component.ddc.configuration.model.DdcPublishMessage;

public record DdcAtomicPublishCommand(
        String configId,
        String changeId,
        String bizCode,
        String env,
        String appCode,
        String resourceName,
        Long expectedPublishedVersion,
        Long targetVersion,
        String content,
        String eventChecksum,
        DdcPublishMessage message
) {
}
