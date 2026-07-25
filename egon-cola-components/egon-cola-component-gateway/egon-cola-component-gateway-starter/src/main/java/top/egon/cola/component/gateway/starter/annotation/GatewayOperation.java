package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayOperation {

    String name() default "";

    String summary() default "";

    String description() default "";

    String owner() default "";

    boolean externalAccessible() default false;

    String[] tags() default {};
}
