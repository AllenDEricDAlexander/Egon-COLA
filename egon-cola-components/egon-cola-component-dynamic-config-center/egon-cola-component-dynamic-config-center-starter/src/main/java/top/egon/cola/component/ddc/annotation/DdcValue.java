package top.egon.cola.component.ddc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DdcValue {

    String value();

    String key() default "";

    String defaultValue() default "";

    Class<?> type() default Object.class;

    boolean required() default false;

    boolean refreshable() default true;
}
