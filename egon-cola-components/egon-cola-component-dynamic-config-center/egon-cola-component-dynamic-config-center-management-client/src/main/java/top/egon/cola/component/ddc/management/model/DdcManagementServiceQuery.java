package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceQuery(
        String env,
        String namespace,
        String serviceKind,
        String protocol,
        String serviceName,
        String group,
        String version
) {
}
