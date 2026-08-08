package top.egon.cola.component.ddc.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明由 Spring 原生配置表达式注入且可由 DDC 选择性刷新的字段。
 * Declares a field injected through a native Spring configuration expression and optionally refreshed by DDC.
 *
 * <p>{@link #value()} 与 {@link Value#value()} 完全一致，支持占位符、嵌套占位符、默认值和
 * Spring 表达式语言。例如 {@code ${order.rate-limit.permits-per-second:100}}。</p>
 *
 * <p>{@link #value()} has the same semantics as {@link Value#value()}, including placeholders,
 * nested placeholders, defaults, and Spring Expression Language. For example,
 * {@code ${order.rate-limit.permits-per-second:100}}.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("")
public @interface DdcValue {

    /**
     * 返回交由 Spring 解析的配置表达式。 Returns the configuration expression resolved by Spring.
     *
     * @return 配置表达式。 the configuration expression
     */
    @AliasFor(annotation = Value.class, attribute = "value")
    String value();

    /**
     * 返回初次注入后是否允许动态刷新该字段。 Returns whether the field may be refreshed after its initial injection.
     *
     * @return 允许动态刷新时为 {@code true}。 {@code true} when runtime refresh is allowed
     */
    boolean refreshable() default true;
}
