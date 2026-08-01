package top.egon.cola.component.ddc.management.model;

public record DdcManagementScopeBinding(
        String bindingId,
        String bizCode,
        String namespaceCode,
        String env,
        String appId,
        String appCode,
        String appName,
        boolean enabled
) {
}
