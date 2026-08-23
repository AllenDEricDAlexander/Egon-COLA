package ${package}.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizationSwaggerConfig {

    @Bean
    public OpenAPI organizationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Management Organization API")
                .version("v1"));
    }
}
