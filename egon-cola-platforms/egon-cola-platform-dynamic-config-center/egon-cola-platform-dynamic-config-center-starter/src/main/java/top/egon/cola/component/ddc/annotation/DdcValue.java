package top.egon.cola.component.ddc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明由 DDC 配置值注入的字段及其解析选项。 Declares a field injected from DDC configuration together with its parsing options.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DdcValue {

    /**
     * 返回兼容占位符写法的配置表达式。 Returns the configuration expression, including the supported placeholder-style form.
     *
     * @return 配置表达式。 the configuration expression
     */
    String value();

    /**
     * 返回覆盖表达式中键名的显式配置键。 Returns the explicit configuration key that overrides the key in the expression.
     *
     * @return 显式配置键，空字符串表示从表达式解析。 the explicit key, or an empty string to parse it from the expression
     */
    String key() default "";

    /**
     * 返回配置缺失时使用的显式默认值。 Returns the explicit default used when the configuration is absent.
     *
     * @return 显式默认值，空字符串表示使用表达式中的默认值。 the explicit default, or an empty string to use the expression default
     */
    String defaultValue() default "";

    /**
     * 返回配置值应转换成的显式类型。 Returns the explicit type to which the configuration value should be converted.
     *
     * @return 目标类型，{@link Object} 表示由解析器采用字符串类型。 the target type; {@link Object} lets the parser use strings
     */
    Class<?> type() default Object.class;

    /**
     * 返回配置缺失时是否应视为错误。 Returns whether a missing configuration value should be treated as an error.
     *
     * @return 必须提供配置时为 {@code true}。 {@code true} when the configuration must be present
     */
    boolean required() default false;

    /**
     * 返回初次注入后是否允许动态刷新该字段。 Returns whether the field may be refreshed after its initial injection.
     *
     * @return 允许动态刷新时为 {@code true}。 {@code true} when runtime refresh is allowed
     */
    boolean refreshable() default true;
}
