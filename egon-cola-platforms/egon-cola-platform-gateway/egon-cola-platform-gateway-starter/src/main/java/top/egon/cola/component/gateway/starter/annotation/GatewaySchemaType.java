package top.egon.cola.component.gateway.starter.annotation;

/**
 * JSON Schema type declared for an individual Java property.
 */
public enum GatewaySchemaType {
    /** Infer the JSON Schema type from the Java property type. */
    AUTO,

    /** A JSON string value. */
    STRING,

    /** A JSON integer value. */
    INTEGER,

    /** A JSON numeric value, including integer values widened to numbers. */
    NUMBER,

    /** A JSON boolean value. */
    BOOLEAN,

    /** A JSON object with named properties. */
    OBJECT,

    /** A JSON array value. */
    ARRAY,

    /** A JSON object representing a string-keyed map. */
    MAP
}
