package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceKey(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        String serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) {
}
