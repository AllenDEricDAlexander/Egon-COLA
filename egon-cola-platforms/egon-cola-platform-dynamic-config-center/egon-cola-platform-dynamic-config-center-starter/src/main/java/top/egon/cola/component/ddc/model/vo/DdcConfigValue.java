package top.egon.cola.component.ddc.model.vo;

/**
 * 从 DDC Admin 读取的单个配置值。
 * / Single configuration value read from DDC Admin.
 */
public class DdcConfigValue {

    /** 配置键。 / Configuration key. */
    private String configKey;

    /** 配置值的文本表示。 / Text representation of the configuration value. */
    private String configValue;

    /** 配置值类型的线协议名称。 / Wire name of the configuration value type. */
    private String valueType;

    /** 配置版本。 / Configuration version. */
    private Long version;

    /**
     * 返回配置键。
     * / Returns the configuration key.
     *
     * @return 配置键 / configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 设置配置键。
     * / Sets the configuration key.
     *
     * @param configKey 配置键 / configuration key
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * 返回配置值文本。
     * / Returns the configuration value text.
     *
     * @return 配置值文本 / configuration value text
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 设置配置值文本。
     * / Sets the configuration value text.
     *
     * @param configValue 配置值文本 / configuration value text
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    /**
     * 返回配置值类型名称。
     * / Returns the configuration value type name.
     *
     * @return 值类型名称 / value type name
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * 设置配置值类型名称。
     * / Sets the configuration value type name.
     *
     * @param valueType 值类型名称 / value type name
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /**
     * 返回配置版本。
     * / Returns the configuration version.
     *
     * @return 配置版本 / configuration version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 设置配置版本。
     * / Sets the configuration version.
     *
     * @param version 配置版本 / configuration version
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}
