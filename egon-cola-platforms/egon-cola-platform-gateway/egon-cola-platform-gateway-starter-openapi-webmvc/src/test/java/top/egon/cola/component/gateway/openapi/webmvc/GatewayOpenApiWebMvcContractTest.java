package top.egon.cola.component.gateway.openapi.webmvc;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.models.GroupedOpenApi;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.config.GatewayOpenApiAutoConfiguration;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = GatewayOpenApiWebMvcContractTest.TestApplication.class,
        properties = {
                "egon.cola.component.gateway.openapi.enabled=true",
                "egon.cola.component.gateway.openapi.biz-code=trade",
                "egon.cola.component.gateway.openapi.application-code=order-service",
                "egon.cola.component.gateway.openapi.resource-uri=https://order-service.example.test",
                "egon.cola.component.gateway.openapi.artifact-version=1.0.0",
                "egon.cola.component.gateway.openapi.build-id=build-1",
                "egon.cola.component.gateway.openapi.published-groups[0]=orders",
                "egon.cola.component.gateway.openapi.published-groups[1]=inventory",
                "springdoc.group-configs[0].group=inventory",
                "springdoc.group-configs[0].paths-to-match[0]=/orders",
                "egon.cola.component.id.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class GatewayOpenApiWebMvcContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void servesAGroupedOpenApi31DocumentOnlyForTheRequiredScope()
            throws Exception {
        org.assertj.core.api.Assertions.assertThat(
                        applicationContext.getBeansOfType(SecurityFilterChain.class))
                .containsKey("gatewayOpenApiWebMvcSecurityFilterChain");
        mockMvc.perform(get("/v3/api-docs/orders")
                        .header("Authorization", "Bearer allowed"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"openapi\":\"3.1.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("orders.list")));
    }

    @Test
    void deniesAnonymousGroupedDocumentRequests() throws Exception {
        mockMvc.perform(get("/v3/api-docs/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesJwtWithoutTheRequiredScope() throws Exception {
        mockMvc.perform(get("/v3/api-docs/orders")
                        .header("Authorization", "Bearer wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void appliesTheSameGovernanceToAPropertyDefinedGroup() throws Exception {
        org.assertj.core.api.Assertions.assertThat(
                        applicationContext.getBeansOfType(GroupedOpenApi.class)
                                .keySet())
                .contains("inventory");
        org.assertj.core.api.Assertions.assertThat(
                        applicationContext.getBean("inventory",
                                GroupedOpenApi.class).getOpenApiCustomizers())
                .isNotEmpty();
        mockMvc.perform(get("/v3/api-docs/inventory")
                        .header("Authorization", "Bearer allowed"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "\"openapiGroup\":\"inventory\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("orders.list")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            GatewayOpenApiAutoConfiguration.class,
            GatewayOpenApiWebMvcSecurityAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("scope", "wrong".equals(token)
                            ? "gateway.other"
                            : "gateway.openapi.read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();
        }

        @Bean
        GroupedOpenApi orders() {
            return GroupedOpenApi.builder()
                    .group("orders")
                    .pathsToMatch("/orders")
                    .build();
        }

        @Bean
        OrderController orderController() {
            return new OrderController();
        }
    }

    @RestController
    @EgonApiCatalog(
            businessDomainCode = "trade",
            entityDomainCode = "order",
            interfaceGroupCode = "orders"
    )
    static class OrderController {

        @GetMapping("/orders")
        @Operation(operationId = "orders.list")
        java.util.Map<String, String> list() {
            return java.util.Map.of("status", "ok");
        }
    }
}
