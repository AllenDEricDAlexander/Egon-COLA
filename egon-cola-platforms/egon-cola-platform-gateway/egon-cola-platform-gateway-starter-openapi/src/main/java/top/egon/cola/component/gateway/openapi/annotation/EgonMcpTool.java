package top.egon.cola.component.gateway.openapi.annotation;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts one OpenAPI operation into the managed MCP projection.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EgonMcpTool {

    boolean enabled() default false;

    String serverCode() default "";

    String name() default "";

    String[] permissions() default {};

    McpRiskLevel riskLevel() default McpRiskLevel.LOW;
}
