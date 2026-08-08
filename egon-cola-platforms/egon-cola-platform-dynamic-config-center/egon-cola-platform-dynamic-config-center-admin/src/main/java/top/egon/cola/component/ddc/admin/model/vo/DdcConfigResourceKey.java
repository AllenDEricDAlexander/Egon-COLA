package top.egon.cola.component.ddc.admin.model.vo;

public record DdcConfigResourceKey(
        String bizCode,
        String env,
        String appCode,
        String resourceName
) {

    public DdcConfigResourceKey {
        bizCode = require(bizCode, "bizCode");
        env = require(env, "env");
        appCode = require(appCode, "appCode");
        resourceName = require(resourceName, "resourceName");
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
