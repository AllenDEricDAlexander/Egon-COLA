package top.egon.cola.component.yuheng.llm.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * 中文说明：LLM 原生 API 的代码优先 OpenAPI 安全定义；接口方法通过同名 bearerAuth 要求企业 SERVICE JWT。
 * English summary: Code-first OpenAPI security definition for the native LLM API; mapped methods require the matching
 * enterprise SERVICE JWT scheme.
 */
@Configuration("llmOpenApiConfiguration")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class LlmOpenApiConfiguration {
}
