package top.egon.cola.component.ddc.model.vo;

import java.lang.reflect.Field;

public class DdcFieldBinding {

    private final Object bean;

    private final Field field;

    private final String configKey;

    private final String defaultValue;

    private final Class<?> targetType;

    private final boolean required;

    private final boolean refreshable;

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

    public Object getBean() {
        return bean;
    }

    public Field getField() {
        return field;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRefreshable() {
        return refreshable;
    }
}
