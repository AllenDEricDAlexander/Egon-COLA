package top.egon.cola.archetype.source.agent.starter.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Code-first metadata for the authenticated Agent operations and their shared security failures. */
@Configuration(value = "deepResearchOpenApiConfiguration", proxyBeanMethods = false)
@SecurityScheme(name = "nativeBearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Slf4j
public class DeepResearchOpenApiConfiguration {

    @Bean(name = "deepResearchOpenApi")
    public OpenAPI deepResearchOpenApi() {
        var error = new ObjectSchema();
        error.addProperty("code", new StringSchema());
        error.addProperty("message", new StringSchema());
        error.setRequired(java.util.List.of("code", "message"));
        error.setAdditionalProperties(false);
        return new OpenAPI()
                .info(new Info().title("Deep Research Agent API").version("v1"))
                .components(new Components().addSecuritySchemes("nativeBearer",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT"))
                        .addSchemas("NativeSecurityError", error)
                        .addSchemas("NativeOrBusinessUnavailableError", new ComposedSchema()
                                .addOneOfItem(new Schema<>().$ref("#/components/schemas/NativeSecurityError"))
                                .addOneOfItem(new Schema<>().$ref("#/components/schemas/DeepResearchErrorResult"))));
    }
}
