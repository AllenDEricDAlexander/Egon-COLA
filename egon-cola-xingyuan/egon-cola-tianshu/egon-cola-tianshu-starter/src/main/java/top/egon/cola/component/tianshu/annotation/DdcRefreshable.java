package top.egon.cola.component.tianshu.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记允许 Tianshu 在运行时重新注入配置字段的 Bean 类型。 Marks a bean type whose configuration fields may be reinjected by Tianshu at runtime.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DdcRefreshable {
}
