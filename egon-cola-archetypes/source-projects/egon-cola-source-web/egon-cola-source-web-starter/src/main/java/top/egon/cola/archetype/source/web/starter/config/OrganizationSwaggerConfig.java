package top.egon.cola.archetype.source.web.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration(value = "organizationSwaggerConfig", proxyBeanMethods = false)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class OrganizationSwaggerConfig {

    @Bean("organizationOpenApi")
    public OpenAPI organizationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Management Organization API")
                .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("nativeBearer", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas("NativeSecurityError", new ObjectSchema()
                                .addProperty("code", new StringSchema()).addProperty("message", new StringSchema())
                                .required(List.of("code", "message")).additionalProperties(false)));
    }
}
