package top.egon.cola.component.gateway.starter.annotation;

/**
 * Structural shape expected for a request, response or payload schema.
 */
public enum GatewaySchemaShape {
    /** Infer the shape from the declared Java type. */
    AUTO,

    /** A scalar value such as a string, number, boolean or enum. */
    VALUE,

    /** A structured object with named properties. */
    OBJECT,

    /** An array or collection whose items share one schema. */
    LIST,

    /** A string-keyed map whose values share one schema. */
    MAP,

    /** The absence of a request or response value. */
    VOID
}
