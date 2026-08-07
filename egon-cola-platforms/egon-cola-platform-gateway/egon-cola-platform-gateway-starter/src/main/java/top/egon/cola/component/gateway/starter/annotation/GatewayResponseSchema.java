package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayResponseSchema {

    Class<?> wrapper() default Void.class;

    String payloadField() default "";

    Class<?> schema() default Void.class;

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;
}
