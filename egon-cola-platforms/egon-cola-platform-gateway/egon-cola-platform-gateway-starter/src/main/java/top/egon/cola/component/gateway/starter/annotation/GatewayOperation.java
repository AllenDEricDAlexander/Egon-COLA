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

    GatewayRequestSchemaField[] requestSchemaFields() default {};

    GatewayResponseSchema responseSchema() default @GatewayResponseSchema;
}
