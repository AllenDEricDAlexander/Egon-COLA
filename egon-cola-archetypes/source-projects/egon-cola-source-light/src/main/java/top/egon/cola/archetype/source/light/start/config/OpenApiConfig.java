package top.egon.cola.archetype.source.light.start.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "openApiConfig", proxyBeanMethods = false)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OpenApiConfig {
    @Bean("studentManagementOpenApi")
    OpenAPI studentManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Management")
                .description("Lite monolith domain architecture sample API")
                .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("nativeBearer", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas("NativeSecurityError", new ObjectSchema()
                                .addProperty("code", new StringSchema()).addProperty("message", new StringSchema())
                                .required(List.of("code", "message")).additionalProperties(false)));
    }
}
