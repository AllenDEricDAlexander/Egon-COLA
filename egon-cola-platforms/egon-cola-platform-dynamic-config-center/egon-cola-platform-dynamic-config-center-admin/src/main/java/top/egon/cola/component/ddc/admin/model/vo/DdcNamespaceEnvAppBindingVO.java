package top.egon.cola.component.ddc.admin.model.vo;

public record DdcNamespaceEnvAppBindingVO(
        String id,
        String bizCode,
        String namespaceId,
        String namespaceCode,
        String env,
        String appId,
        String appCode,
        String appName,
        boolean enabled
) {
}
