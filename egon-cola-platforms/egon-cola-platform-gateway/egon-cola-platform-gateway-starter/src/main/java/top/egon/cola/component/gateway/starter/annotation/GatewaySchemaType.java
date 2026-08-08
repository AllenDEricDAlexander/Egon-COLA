package top.egon.cola.component.gateway.starter.annotation;

/**
 * JSON Schema type declared for an individual Java property.
 *
 * <p>定义单个 Java 属性声明的 JSON Schema 类型。
 */
public enum GatewaySchemaType {
    /** Infer the JSON Schema type from the Java property type. 根据 Java 属性类型推断 JSON Schema 类型。 */
    AUTO,

    /** A JSON string value. JSON 字符串值。 */
    STRING,

    /** A JSON integer value. JSON 整数值。 */
    INTEGER,

    /** A JSON numeric value, including integer values widened to numbers. JSON 数值，也包括扩展为 number 的整数值。 */
    NUMBER,

    /** A JSON boolean value. JSON 布尔值。 */
    BOOLEAN,

    /** A JSON object with named properties. 带有命名属性的 JSON 对象。 */
    OBJECT,

    /** A JSON array value. JSON 数组值。 */
    ARRAY,

    /** A JSON object representing a string-keyed map. 表示字符串键映射的 JSON 对象。 */
    MAP
}
