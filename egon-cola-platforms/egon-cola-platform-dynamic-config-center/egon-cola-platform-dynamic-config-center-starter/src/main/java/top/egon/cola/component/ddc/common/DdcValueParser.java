package top.egon.cola.component.ddc.common;

public final class DdcValueParser {

    private DdcValueParser() {
    }

    public static DdcValueDefinition parse(String expression, String explicitKey, String explicitDefaultValue, Class<?> explicitType) {
        String key = hasText(explicitKey) ? explicitKey.trim() : expressionKey(expression);
        if (!hasText(key)) {
            throw new DdcException("config key must not be blank");
        }
        String defaultValue = hasText(explicitDefaultValue) ? explicitDefaultValue : expressionDefault(expression);
        Class<?> type = explicitType == null || explicitType == Object.class ? String.class : explicitType;
        return new DdcValueDefinition(key, defaultValue, type);
    }

    private static String expressionKey(String expression) {
        if (!hasText(expression)) {
            return "";
        }
        int separator = expression.indexOf(':');
        String key = separator < 0 ? expression : expression.substring(0, separator);
        return key.trim();
    }

    private static String expressionDefault(String expression) {
        if (expression == null) {
            return "";
        }
        int separator = expression.indexOf(':');
        return separator < 0 ? "" : expression.substring(separator + 1);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
