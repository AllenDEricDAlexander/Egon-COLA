package top.egon.cola.component.ddc.management.model;

public record DdcManagementInstanceQuery(
        String appCode,
        String env,
        String namespace
) {
}
