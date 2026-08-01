package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceQuery(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        String serviceKind,
        String protocol,
        String serviceName,
        String group,
        String version
) {
}
