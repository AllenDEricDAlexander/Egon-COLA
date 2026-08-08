package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the schema of an HTTP response and its business payload wrapping.
 *
 * <p>RPC output schemas are generated from Protobuf descriptors and must not be
 * duplicated with this annotation.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayResponseSchema {

    /**
     * Java class of the response wrapper.
     *
     * <p>{@link Void} denotes a direct, unwrapped response.
     *
     * @return the wrapper class, or {@link Void} for a direct response
     */
    Class<?> wrapper() default Void.class;

    /**
     * Name of the wrapper property that contains the business payload.
     *
     * <p>This value is required when {@link #wrapper()} names a wrapper and must
     * remain blank for a direct response.
     *
     * @return the payload property name, or an empty string for a direct response
     */
    String payloadField() default "";

    /**
     * Java class represented by the response or wrapped payload schema.
     *
     * <p>For list and map shapes this is the element or value class. {@link Void}
     * is also used by the all-default declaration and by a void response.
     *
     * @return the declared response schema class
     */
    Class<?> schema() default Void.class;

    /**
     * Expected structural shape of the response or wrapped payload.
     *
     * @return the declared shape, or {@link GatewaySchemaShape#AUTO} to infer it
     */
    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;
}
