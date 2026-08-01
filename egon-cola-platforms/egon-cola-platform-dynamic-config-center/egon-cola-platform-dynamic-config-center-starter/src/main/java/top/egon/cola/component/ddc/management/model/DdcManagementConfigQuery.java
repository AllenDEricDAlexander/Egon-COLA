package top.egon.cola.component.ddc.management.model;

public record DdcManagementConfigQuery(
        String bizCode,
        String env,
        String appCode,
        String configKey
) {

    public DdcManagementConfigQuery {
        bizCode = required(bizCode, "bizCode");
        env = required(env, "env");
        appCode = required(appCode, "appCode");
        configKey = required(configKey, "configKey");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
