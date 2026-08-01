package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigDeleteRequest(
        String bizCode,
        String env,
        String appCode,
        String configKey,
        Long expectedVersion,
        String operator,
        String reason
) {
}
