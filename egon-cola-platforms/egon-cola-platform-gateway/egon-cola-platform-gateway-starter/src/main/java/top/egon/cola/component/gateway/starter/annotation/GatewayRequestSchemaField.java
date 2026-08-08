package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the root schema of one HTTP request parameter or request body.
 *
 * <p>RPC input schemas are generated from Protobuf descriptors and must not be
 * duplicated with this annotation.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRequestSchemaField {

    /**
     * Protocol location of the declared request value.
     *
     * @return the HTTP request location
     */
    GatewayRequestLocation location();

    /**
     * Java class represented by the declaration.
     *
     * <p>For list and map shapes this is the element or value class; otherwise it
     * is the request value class itself.
     *
     * @return the declared schema class
     */
    Class<?> schema();

    /**
     * Bound request parameter name.
     *
     * <p>The name is empty for request bodies and expanded model attributes.
     *
     * @return the parameter name, or an empty string when the declaration has no
     *         single bound name
     */
    String name() default "";

    /**
     * Expected structural shape of the request value.
     *
     * @return the declared shape, or {@link GatewaySchemaShape#AUTO} to infer it
     */
    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;

    /**
     * Whether an object-valued query parameter is expanded into individual query
     * properties.
     *
     * <p>Expansion is valid only for a query declaration with the
     * {@link GatewaySchemaShape#OBJECT} shape and a blank {@link #name()}.
     *
     * @return {@code true} for an expanded query model attribute
     */
    boolean expanded() default false;
}
