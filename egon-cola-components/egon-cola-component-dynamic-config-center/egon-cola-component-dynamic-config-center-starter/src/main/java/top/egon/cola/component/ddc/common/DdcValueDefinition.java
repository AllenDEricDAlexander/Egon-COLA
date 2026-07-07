package top.egon.cola.component.ddc.common;

public class DdcValueDefinition {

    private final String key;

    private final String defaultValue;

    private final Class<?> type;

    public DdcValueDefinition(String key, String defaultValue, Class<?> type) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getType() {
        return type;
    }
}
