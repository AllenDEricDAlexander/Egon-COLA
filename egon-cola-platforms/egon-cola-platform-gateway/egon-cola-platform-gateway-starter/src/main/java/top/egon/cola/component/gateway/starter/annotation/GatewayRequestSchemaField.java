package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRequestSchemaField {

    GatewayRequestLocation location();

    Class<?> schema();

    String name() default "";

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;

    boolean expanded() default false;
}
