package top.egon.cola.component.tianshu.admin.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = "tianshuBearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
public class DdcAiOpenApiConfiguration {

    @Bean(name = "ddcAiSchemaOpenApiCustomizer")
    public GlobalOpenApiCustomizer ddcAiSchemaOpenApiCustomizer() {
        return api -> {
            ensureRequiredNullable(api, "DdcAiContentDTO", List.of("promptText", "nodes"), "promptText");
            ensureRequiredNullable(api, "DdcAiNodeDTO", List.of("content"), "content");
            addNoStoreResponseHeaders(api);
        };
    }

    private void addNoStoreResponseHeaders(OpenAPI api) {
        if (api.getPaths() == null) return;
        api.getPaths().forEach((path, pathItem) -> {
            if (!path.startsWith("/api/v1/tianshu/ai/")) return;
            for (Operation operation : pathItem.readOperations()) {
                if (operation.getResponses() == null) continue;
                for (ApiResponse response : operation.getResponses().values()) {
                    Header existing = response.getHeaders() == null ? null
                            : response.getHeaders().get("Cache-Control");
                    if (existing == null) {
                        if (response.getHeaders() == null) response.setHeaders(new LinkedHashMap<>());
                        response.getHeaders().put("Cache-Control", new Header()
                                .description("Sensitive AI response cache policy")
                                .schema(new StringSchema().example("no-store")));
                    }
                }
            }
        });
    }

    private void ensureRequiredNullable(OpenAPI api, String schemaName, List<String> required,
            String nullableProperty) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) return;
        Schema<?> schema = api.getComponents().getSchemas().get(schemaName);
        if (schema == null) return;
        required.forEach(schema::addRequiredItem);
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && properties.get(nullableProperty) != null) {
            Schema<?> nullableSchema = properties.get(nullableProperty);
            nullableSchema.setType(null);
            nullableSchema.setTypes(Set.of("string", "null"));
            nullableSchema.setNullable(null);
        }
    }
}
