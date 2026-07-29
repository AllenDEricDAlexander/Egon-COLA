package top.egon.cola.component.accessguard.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiterAccessInterceptor {

    String name() default "";

    String key() default "all";

    String keyExpression() default "";

    double permitsPerSecond() default -1.0d;

    long permits() default 1L;

    long interval() default 1L;

    TimeUnit intervalUnit() default TimeUnit.SECONDS;

    long blacklistCount() default 0L;

    long blacklistTimeout() default 24L;

    TimeUnit blacklistTimeUnit() default TimeUnit.HOURS;

    String fallbackMethod() default "";

    String returnJson() default "";

    FailStrategy failStrategy() default FailStrategy.GLOBAL_DEFAULT;

    boolean enableBlacklistForAllKey() default false;
}
