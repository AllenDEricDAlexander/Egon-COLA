package top.egon.cola.component.ddc.admin.model.vo;

public record DdcConfigResourceKey(
        String appCode,
        String env,
        String namespace,
        String configKey
) {

    public DdcConfigResourceKey {
        appCode = require(appCode, "appCode");
        env = require(env, "env");
        namespace = require(namespace, "namespace");
        configKey = require(configKey, "configKey");
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
