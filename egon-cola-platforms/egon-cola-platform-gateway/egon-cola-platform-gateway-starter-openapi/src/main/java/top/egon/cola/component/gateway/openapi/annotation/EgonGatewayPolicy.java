package top.egon.cola.component.gateway.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds governance metadata that standard OpenAPI operation annotations do not
 * own.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EgonGatewayPolicy {

    String owner() default "";

    Exposure exposure() default Exposure.INTERNAL;

    Idempotency idempotency() default Idempotency.AUTO;

    enum Exposure {
        INTERNAL,
        EXTERNAL
    }

    enum Idempotency {
        AUTO,
        TRUE,
        FALSE
    }
}
