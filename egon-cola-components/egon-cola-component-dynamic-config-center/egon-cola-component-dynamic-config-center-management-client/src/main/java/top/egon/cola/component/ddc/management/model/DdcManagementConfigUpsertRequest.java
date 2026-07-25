package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigUpsertRequest(
        String appCode,
        String env,
        String namespace,
        String configKey,
        String configValue,
        String valueType,
        String description,
        Long expectedVersion,
        String operator
) {
}
