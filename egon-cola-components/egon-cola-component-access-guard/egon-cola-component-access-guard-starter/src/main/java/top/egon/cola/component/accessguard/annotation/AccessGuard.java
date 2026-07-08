package top.egon.cola.component.accessguard.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessGuard {

    String name() default "";

    String key() default "all";

    String keyExpression() default "";

    boolean whitelist() default false;

    boolean rateLimiter() default false;

    boolean blacklist() default false;

    boolean timeoutBreaker() default false;

    String fallbackMethod() default "";

    String returnJson() default "";
}
