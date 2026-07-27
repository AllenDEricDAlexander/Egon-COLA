package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceKey(
        String env,
        String namespace,
        String serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) {
}
