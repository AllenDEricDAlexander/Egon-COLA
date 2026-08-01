package top.egon.cola.component.ddc.management.model;

public record DdcManagementScopeQuery(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode
) {
}
