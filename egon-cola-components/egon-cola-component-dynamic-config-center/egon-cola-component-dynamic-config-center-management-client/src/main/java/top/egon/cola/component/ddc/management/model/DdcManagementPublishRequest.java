package top.egon.cola.component.ddc.management.model;

public record DdcManagementPublishRequest(
        String appCode,
        String env,
        String namespace,
        String configKey,
        String configValue,
        Long expectedVersion,
        String changeId,
        Long timeoutMs,
        String operator
) {
}
