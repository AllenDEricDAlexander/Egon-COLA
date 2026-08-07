package top.egon.cola.component.gateway.starter.annotation;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayOperation {

    String name() default "";

    String summary() default "";

    String description() default "";

    String owner() default "";

    boolean externalAccessible() default false;

    boolean idempotent() default false;

    boolean registerMcp() default false;

    String mcpName() default "";

    String[] mcpRequiredPermissions() default {};

    McpRiskLevel mcpRiskLevel() default McpRiskLevel.LOW;

    String[] tags() default {};

    /**
     * HTTP 请求参数的完整 Schema 声明；RPC 请求 Schema 由 Protobuf Descriptor 自动生成。
     */
    GatewayRequestSchemaField[] requestSchemaFields() default {};

    /**
     * HTTP 响应及包装对象的 Schema 声明；RPC 响应 Schema 由 Protobuf Descriptor 自动生成。
     */
    GatewayResponseSchema responseSchema() default @GatewayResponseSchema;
}
