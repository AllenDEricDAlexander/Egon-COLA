package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigUpsertRequest(
        String bizCode,
        String env,
        String appCode,
        String configValue,
        String description,
        Long expectedVersion,
        String operator
) {
}
