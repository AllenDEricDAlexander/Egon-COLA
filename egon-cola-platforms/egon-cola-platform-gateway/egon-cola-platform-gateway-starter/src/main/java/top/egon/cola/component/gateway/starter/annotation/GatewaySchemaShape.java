package top.egon.cola.component.gateway.starter.annotation;

/**
 * Structural shape expected for a request, response or payload schema.
 *
 * <p>定义请求、响应或载荷模式所期望的结构形状。
 */
public enum GatewaySchemaShape {
    /** Infer the shape from the declared Java type. 根据声明的 Java 类型推断结构形状。 */
    AUTO,

    /** A scalar value such as a string, number, boolean or enum. 字符串、数字、布尔值或枚举等标量值。 */
    VALUE,

    /** A structured object with named properties. 带有命名属性的结构化对象。 */
    OBJECT,

    /** An array or collection whose items share one schema. 元素共用一个模式的数组或集合。 */
    LIST,

    /** A string-keyed map whose values share one schema. 键为字符串且值共用一个模式的映射。 */
    MAP,

    /** The absence of a request or response value. 表示请求或响应值不存在。 */
    VOID
}
