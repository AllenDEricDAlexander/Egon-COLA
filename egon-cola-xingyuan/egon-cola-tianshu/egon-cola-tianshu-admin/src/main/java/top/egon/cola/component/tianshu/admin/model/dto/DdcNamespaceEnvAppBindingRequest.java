package top.egon.cola.component.tianshu.admin.model.dto;

public record DdcNamespaceEnvAppBindingRequest(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        Boolean enabled
) {
}
