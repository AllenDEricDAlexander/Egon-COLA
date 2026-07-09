package ${package}.start.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean
    OpenAPI studentManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Management")
                .description("Lite monolith domain architecture sample API")
                .version("1.0.0"));
    }
}
