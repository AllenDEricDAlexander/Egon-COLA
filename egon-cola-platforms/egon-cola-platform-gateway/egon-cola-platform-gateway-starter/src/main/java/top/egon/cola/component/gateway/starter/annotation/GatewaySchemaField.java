package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.METHOD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewaySchemaField {

    String description() default "";

    GatewaySchemaType type() default GatewaySchemaType.AUTO;

    String format() default "";

    GatewaySchemaRequired required() default GatewaySchemaRequired.AUTO;

    String example() default "";

    Class<?> implementation() default Void.class;
}
