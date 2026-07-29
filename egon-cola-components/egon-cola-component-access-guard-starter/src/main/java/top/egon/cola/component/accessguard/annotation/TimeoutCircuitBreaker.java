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
public @interface TimeoutCircuitBreaker {

    String name() default "";

    long timeoutValue() default 500L;

    TimeUnit timeoutUnit() default TimeUnit.MILLISECONDS;

    String fallbackMethod() default "";

    String returnJson() default "";

    TimeoutExecutorType executor() default TimeoutExecutorType.GLOBAL_DEFAULT;

    String threadPoolName() default "";

    int corePoolSize() default -1;

    int maxPoolSize() default -1;

    int queueCapacity() default -1;

    boolean fallbackOnException() default false;

    boolean cancelRunningTask() default true;

    boolean enabled() default true;
}
