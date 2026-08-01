package top.egon.cola.component.ddc.admin.model.vo;

public record DdcConfigResourceKey(
        String bizCode,
        String env,
        String appCode,
        String configKey
) {

    public DdcConfigResourceKey {
        bizCode = require(bizCode, "bizCode");
        env = require(env, "env");
        appCode = require(appCode, "appCode");
        configKey = require(configKey, "configKey");
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
