package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigDeleteRequest(
        String appCode,
        String env,
        String namespace,
        String configKey,
        Long expectedVersion,
        String operator,
        String reason
) {
}
