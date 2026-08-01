package top.egon.cola.component.ddc.admin.model.dto;

public record DdcNamespaceEnvAppBindingRequest(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        Boolean enabled
) {
}
