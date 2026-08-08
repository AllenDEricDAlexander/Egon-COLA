package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds gateway JSON Schema metadata to a Java property or request parameter.
 *
 * <p>Declarations discovered on a field, accessor, constructor parameter or
 * record component are merged for the logical property. Conflicting declarations
 * on those elements are rejected during schema discovery.
 */
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.METHOD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewaySchemaField {

    /**
     * Human-readable description published for the schema field.
     *
     * @return the description, or an empty string when unspecified
     */
    String description() default "";

    /**
     * JSON Schema type expected for the Java field.
     *
     * <p>An explicit value is validated against the inferred Java type; it does
     * not coerce an incompatible value.
     *
     * @return the declared schema type, or {@link GatewaySchemaType#AUTO} to infer
     *         it
     */
    GatewaySchemaType type() default GatewaySchemaType.AUTO;

    /**
     * JSON Schema format keyword published for the field.
     *
     * @return the format, or an empty string when unspecified
     */
    String format() default "";

    /**
     * Requiredness override for the schema property.
     *
     * @return the requiredness policy
     */
    GatewaySchemaRequired required() default GatewaySchemaRequired.AUTO;

    /**
     * Example value encoded as text.
     *
     * <p>The value is parsed and validated against the effective schema type
     * during discovery.
     *
     * @return the encoded example, or an empty string when unspecified
     */
    String example() default "";

    /**
     * Concrete implementation used to describe an abstract, interface or
     * {@link Object}-typed property.
     *
     * <p>For collection and map properties, this class describes the element or
     * value implementation. Incompatible or redundant implementations are
     * rejected.
     *
     * @return the concrete implementation, or {@link Void} when none is declared
     */
    Class<?> implementation() default Void.class;
}
