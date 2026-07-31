package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceQuery(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        String serviceKind,
        String protocol,
        String serviceName,
        String group,
        String version
) {
}
