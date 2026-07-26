package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigQuery(
        String appCode,
        String env,
        String namespace,
        String configKey
) {

    public DdcManagementConfigQuery {
        appCode = required(appCode, "appCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        configKey = required(configKey, "configKey");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
