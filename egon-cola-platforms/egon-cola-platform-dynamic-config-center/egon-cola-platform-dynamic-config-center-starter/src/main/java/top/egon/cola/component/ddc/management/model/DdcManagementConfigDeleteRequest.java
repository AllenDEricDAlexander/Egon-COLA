package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigDeleteRequest(
        String bizCode,
        String env,
        String appCode,
        Long expectedVersion,
        String operator,
        String reason
) {
}
