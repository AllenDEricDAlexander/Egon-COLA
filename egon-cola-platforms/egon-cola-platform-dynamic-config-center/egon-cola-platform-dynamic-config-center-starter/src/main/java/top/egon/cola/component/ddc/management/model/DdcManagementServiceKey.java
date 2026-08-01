package top.egon.cola.component.ddc.management.model;

public record DdcManagementServiceKey(
        String bizCode,
        String env,
        String appCode,
        String serviceId,
        String serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) {
}
