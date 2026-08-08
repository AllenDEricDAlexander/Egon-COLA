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
 *
 * <p>声明一个 HTTP 请求参数或请求体的根模式；RPC 输入模式由 Protobuf 描述符生成，
 * 不应使用此注解重复声明。
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRequestSchemaField {

    /**
     * Protocol location of the declared request value.
     *
     * <p>所声明请求值在 HTTP 协议中的位置。
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
     * <p>声明对应的 Java 类型；列表和映射形状下表示元素类型或值类型，否则表示请求值本身。
     *
     * @return the declared schema class
     */
    Class<?> schema();

    /**
     * Bound request parameter name.
     *
     * <p>The name is empty for request bodies and expanded model attributes.
     *
     * <p>绑定的请求参数名；请求体和展开的模型属性没有单独绑定名称时为空。
     *
     * @return the parameter name, or an empty string when the declaration has no
     *         single bound name
     */
    String name() default "";

    /**
     * Expected structural shape of the request value.
     *
     * <p>请求值的结构形状；使用 {@link GatewaySchemaShape#AUTO} 时根据类型推断。
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
     * <p>对象类型查询参数是否展开为独立的查询属性。
     *
     * @return {@code true} for an expanded query model attribute
     */
    boolean expanded() default false;
}
