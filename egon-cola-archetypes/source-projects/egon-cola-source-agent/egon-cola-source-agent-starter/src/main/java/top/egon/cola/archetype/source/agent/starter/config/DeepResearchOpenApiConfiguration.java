package top.egon.cola.archetype.source.agent.starter.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Code-first OpenAPI metadata for the single authenticated Deep Research operation. */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = "researchApiKey", type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER, paramName = "X-Research-Api-Key")
public class DeepResearchOpenApiConfiguration {

    @Bean(name = "deepResearchOpenApi")
    public OpenAPI deepResearchOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Deep Research Agent API").version("v1"))
                .components(new Components().addSecuritySchemes("researchApiKey",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                                .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                                .name("X-Research-Api-Key")));
    }
}
