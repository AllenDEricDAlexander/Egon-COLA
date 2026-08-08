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
 *
 * <p>声明 HTTP 响应及其业务载荷包装的模式；RPC 输出模式由 Protobuf 描述符生成，
 * 不应使用此注解重复声明。
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
     * <p>响应包装器的 Java 类型；{@link Void} 表示不包装、直接返回响应。
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
     * <p>承载业务载荷的包装属性名；包装器存在时必须提供，直接响应时必须为空。
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
     * <p>响应或包装载荷对应的 Java 类型；列表和映射形状下表示元素类型或值类型。
     *
     * @return the declared response schema class
     */
    Class<?> schema() default Void.class;

    /**
     * Expected structural shape of the response or wrapped payload.
     *
     * <p>响应或包装载荷的结构形状；使用 {@link GatewaySchemaShape#AUTO} 时根据类型推断。
     *
     * @return the declared shape, or {@link GatewaySchemaShape#AUTO} to infer it
     */
    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;
}
