package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP 返回值及其业务数据包装关系的 Schema 声明。
 *
 * <p>RPC 输出由 Protobuf Descriptor 自动生成，不允许使用该注解重复声明。
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayResponseSchema {

    Class<?> wrapper() default Void.class;

    String payloadField() default "";

    Class<?> schema() default Void.class;

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;
}
