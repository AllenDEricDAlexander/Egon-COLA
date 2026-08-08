package top.egon.cola.component.ddc.management.model;

/**
 * 精确定位配置的完整作用域查询。 / Complete scope query that identifies one configuration.
 *
 * @param bizCode 必填业务编码 / required business code
 * @param env 必填环境编码 / required environment code
 * @param appCode 必填应用编码 / required application code
 */
public record DdcManagementConfigQuery(
        String bizCode,
        String env,
        String appCode
) {

    /**
     * 校验并构造完整配置作用域。 / Validates and constructs a complete configuration scope.
     *
     * @throws IllegalArgumentException 当任一作用域字段为空时 / when any scope field is blank
     */
    public DdcManagementConfigQuery {
        bizCode = required(bizCode, "bizCode");
        env = required(env, "env");
        appCode = required(appCode, "appCode");
    }

    /**
     * 校验必填文本。 / Validates required text.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始非空文本 / original nonblank text
     * @throws IllegalArgumentException 当值为空时 / when the value is blank
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
