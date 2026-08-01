package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigUpsertRequest(
        String bizCode,
        String env,
        String appCode,
        String configKey,
        String configValue,
        String valueType,
        String description,
        Long expectedVersion,
        String operator
) {
}
