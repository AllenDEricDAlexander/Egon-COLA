package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP 方法中一个请求参数或请求体的根 Schema 声明。
 *
 * <p>RPC 输入由 Protobuf Descriptor 自动生成，不允许使用该注解重复声明。
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayRequestSchemaField {

    GatewayRequestLocation location();

    Class<?> schema();

    String name() default "";

    GatewaySchemaShape shape() default GatewaySchemaShape.AUTO;

    boolean expanded() default false;
}
