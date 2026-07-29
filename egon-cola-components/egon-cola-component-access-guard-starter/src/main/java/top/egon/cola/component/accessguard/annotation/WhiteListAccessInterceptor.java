package top.egon.cola.component.accessguard.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WhiteListAccessInterceptor {

    String name() default "";

    String key() default "all";

    String keyExpression() default "";

    String[] users() default {};

    String fallbackMethod() default "";

    String returnJson() default "";

    WhiteListMode mode() default WhiteListMode.GATEKEEPER;

    FailStrategy failStrategy() default FailStrategy.GLOBAL_DEFAULT;

    boolean enabled() default true;
}
