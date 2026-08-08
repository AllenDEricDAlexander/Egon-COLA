package top.egon.cola.component.ddc.common;

/**
 * 解析 DDC 字段表达式，并合并显式键、默认值和类型选项。 Parses DDC field expressions and merges explicit key, default, and type options.
 */
public final class DdcValueParser {

    /**
     * 禁止实例化表达式解析工具类。 Prevents instantiation of the expression parser utility.
     */
    private DdcValueParser() {
    }

    /**
     * 解析表达式并让显式选项优先于表达式内容。 Parses an expression with explicit options taking precedence over expression content.
     *
     * @param expression           可包含冒号默认值的表达式。 expression that may contain a colon-delimited default
     * @param explicitKey          显式配置键。 explicit configuration key
     * @param explicitDefaultValue 显式默认值。 explicit default value
     * @param explicitType         显式目标类型。 explicit target type
     * @return 规范化配置值定义。 normalized configuration-value definition
     * @throws DdcException 最终配置键为空时抛出。 thrown when the resulting configuration key is blank
     */
    public static DdcValueDefinition parse(String expression, String explicitKey, String explicitDefaultValue, Class<?> explicitType) {
        String key = hasText(explicitKey) ? explicitKey.trim() : expressionKey(expression);
        if (!hasText(key)) {
            throw new DdcException("config key must not be blank");
        }
        String defaultValue = hasText(explicitDefaultValue) ? explicitDefaultValue : expressionDefault(expression);
        Class<?> type = explicitType == null || explicitType == Object.class ? String.class : explicitType;
        return new DdcValueDefinition(key, defaultValue, type);
    }

    /**
     * 提取表达式中第一个冒号之前的配置键。 Extracts the configuration key before the first colon in an expression.
     *
     * @param expression 配置表达式。 configuration expression
     * @return 去除首尾空白的键，无有效表达式时为空字符串。 trimmed key, or an empty string for no usable expression
     */
    private static String expressionKey(String expression) {
        if (!hasText(expression)) {
            return "";
        }
        int separator = expression.indexOf(':');
        String key = separator < 0 ? expression : expression.substring(0, separator);
        return key.trim();
    }

    /**
     * 提取表达式中第一个冒号之后的完整默认文本。 Extracts the complete default text after the first colon in an expression.
     *
     * @param expression 配置表达式。 configuration expression
     * @return 默认文本，不含冒号时为空字符串。 default text, or an empty string when no colon exists
     */
    private static String expressionDefault(String expression) {
        if (expression == null) {
            return "";
        }
        int separator = expression.indexOf(':');
        return separator < 0 ? "" : expression.substring(separator + 1);
    }

    /**
     * 判断字符串是否包含非空白字符。 Determines whether a string contains non-whitespace characters.
     *
     * @param value 待判断文本。 text to inspect
     * @return 包含有效文本时为 {@code true}。 {@code true} when usable text is present
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
