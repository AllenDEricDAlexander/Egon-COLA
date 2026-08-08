package top.egon.cola.component.ddc.model.vo;

import java.lang.reflect.Field;

/**
 * 将 Spring Bean 字段与 DDC 配置键关联的不可变绑定描述。
 * / Immutable binding descriptor between a Spring bean field and a DDC configuration key.
 */
public class DdcFieldBinding {

    /** 持有目标字段的 Bean 实例。 / Bean instance that owns the target field. */
    private final Object bean;

    /** 接收配置值的反射字段。 / Reflective field that receives the configuration value. */
    private final Field field;

    /** DDC 配置键。 / DDC configuration key. */
    private final String configKey;

    /** 未获取远端值时使用的默认文本。 / Default text used when no remote value is available. */
    private final String defaultValue;

    /** 配置值转换后的目标类型。 / Target type after configuration value conversion. */
    private final Class<?> targetType;

    /** 是否要求配置值必须存在。 / Whether a configuration value is required. */
    private final boolean required;

    /** 是否允许运行时刷新该字段。 / Whether the field may be refreshed at runtime. */
    private final boolean refreshable;

    /**
     * 创建字段绑定描述。
     * / Creates a field binding descriptor.
     *
     * @param bean 持有字段的 Bean 实例 / bean instance that owns the field
     * @param field 接收配置值的字段 / field that receives the configuration value
     * @param configKey DDC 配置键 / DDC configuration key
     * @param defaultValue 默认配置文本 / default configuration text
     * @param targetType 转换目标类型 / conversion target type
     * @param required 是否为必填配置 / whether the configuration is required
     * @param refreshable 是否允许运行时刷新 / whether runtime refresh is allowed
     */
    public DdcFieldBinding(Object bean, Field field, String configKey, String defaultValue,
                           Class<?> targetType, boolean required, boolean refreshable) {
        this.bean = bean;
        this.field = field;
        this.configKey = configKey;
        this.defaultValue = defaultValue;
        this.targetType = targetType;
        this.required = required;
        this.refreshable = refreshable;
    }

    /**
     * 返回持有目标字段的 Bean。
     * / Returns the bean that owns the target field.
     *
     * @return Bean 实例 / bean instance
     */
    public Object getBean() {
        return bean;
    }

    /**
     * 返回接收配置值的字段。
     * / Returns the field that receives the configuration value.
     *
     * @return 反射字段 / reflective field
     */
    public Field getField() {
        return field;
    }

    /**
     * 返回 DDC 配置键。
     * / Returns the DDC configuration key.
     *
     * @return 配置键 / configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 返回默认配置文本。
     * / Returns the default configuration text.
     *
     * @return 默认配置文本 / default configuration text
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 返回配置值转换后的目标类型。
     * / Returns the target type after configuration value conversion.
     *
     * @return 目标类型 / target type
     */
    public Class<?> getTargetType() {
        return targetType;
    }

    /**
     * 判断该配置是否必填。
     * / Indicates whether the configuration is required.
     *
     * @return 必填时为 {@code true} / {@code true} when required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 判断字段是否允许运行时刷新。
     * / Indicates whether the field may be refreshed at runtime.
     *
     * @return 可刷新时为 {@code true} / {@code true} when refreshable
     */
    public boolean isRefreshable() {
        return refreshable;
    }
}
