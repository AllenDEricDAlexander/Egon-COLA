package top.egon.cola.component.ddc.management.model;

public record DdcManagementPublishRequest(
        String bizCode,
        String env,
        String appCode,
        String configValue,
        Long expectedVersion,
        String changeId,
        Long timeoutMs,
        String operator
) {
}
