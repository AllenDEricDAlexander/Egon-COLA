package top.egon.cola.component.ddc.common;

/**
 * 保存字段注入所需的规范化配置键、默认值和目标类型。 Holds the normalized configuration key, default value, and target type required for field injection.
 */
public class DdcValueDefinition {

    /** 规范化配置键。 Normalized configuration key. */
    private final String key;

    /** 配置缺失时使用的默认文本。 Default text used when configuration is absent. */
    private final String defaultValue;

    /** 配置文本的转换目标类型。 Target type for configuration-text conversion. */
    private final Class<?> type;

    /**
     * 创建不可变的配置值定义。 Creates an immutable configuration-value definition.
     *
     * @param key 配置键。 configuration key
     * @param defaultValue 默认文本。 default text
     * @param type 目标类型。 target type
     */
    public DdcValueDefinition(String key, String defaultValue, Class<?> type) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    /**
     * 返回规范化配置键。 Returns the normalized configuration key.
     * @return 规范化配置键。 normalized configuration key
     */
    public String getKey() {
        return key;
    }

    /**
     * 返回配置缺失时使用的默认文本。 Returns the default text used when configuration is absent.
     * @return 默认文本。 default text
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 返回配置转换目标类型。 Returns the configuration conversion target type.
     * @return 目标类型。 target type
     */
    public Class<?> getType() {
        return type;
    }
}
