package top.egon.cola.component.yuheng.openapi.webmvc;

import io.swagger.v3.oas.annotations.Operation;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springdoc.core.models.GroupedOpenApi;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayOpenApiWebMvcContractTest {
    private static final URI RESOURCE = URI.create("https://order-service.example.test");
    @TempDir Path temporary;
    private final GatewayOpenApiWebMvcSecurityAutoConfigurationTest fixture = new GatewayOpenApiWebMvcSecurityAutoConfigurationTest();

    @BeforeEach
    void setup() throws Exception {
        fixture.temporary = temporary;
        fixture.setupPipeline(RESOURCE, Map.ofEntries(
                Map.entry("egon.cola.component.yuheng.openapi.enabled", true),
                Map.entry("egon.cola.component.yuheng.openapi.biz-code", "trade"),
                Map.entry("egon.cola.component.yuheng.openapi.application-code", "order-service"),
                Map.entry("egon.cola.component.yuheng.openapi.resource-uri", RESOURCE.toASCIIString()),
                Map.entry("egon.cola.component.yuheng.openapi.artifact-version", "1.0.0"),
                Map.entry("egon.cola.component.yuheng.openapi.build-id", "build-1"),
                Map.entry("egon.cola.component.yuheng.openapi.published-groups[0]", "orders"),
                Map.entry("egon.cola.component.yuheng.openapi.published-groups[1]", "inventory"),
                Map.entry("springdoc.group-configs[0].group", "inventory"),
                Map.entry("springdoc.group-configs[0].paths-to-match[0]", "/orders"),
                Map.entry("egon.cola.component.id.enabled", false),
                Map.entry("spring.autoconfigure.exclude", "org.redisson.spring.starter.RedissonAutoConfigurationV2,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration")
        ), TestApplication.class);
    }

    @AfterEach
    void cleanup() { fixture.cleanup(); }

    @Test
    void servesAGroupedOpenApi31DocumentOnlyForTheRequiredScope() throws Exception {
        assertThat(fixture.context.getBeansOfType(SecurityFilterChain.class)).containsKey("gatewayOpenApiWebMvcSecurityFilterChain");
        fixture.mvc.perform(get("/v3/api-docs/orders").header("Authorization", credential("yuheng.openapi.read")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"openapi\":\"3.1.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("orders.list")));
    }

    @Test
    void deniesAnonymousGroupedDocumentRequests() throws Exception {
        fixture.mvc.perform(get("/v3/api-docs/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void deniesJwtWithoutTheRequiredScope() throws Exception {
        fixture.mvc.perform(get("/v3/api-docs/orders").header("Authorization", credential("yuheng.other")))
                .andExpect(status().isForbidden());
    }

    @Test
    void appliesTheSameGovernanceToAPropertyDefinedGroup() throws Exception {
        assertThat(fixture.context.getBeansOfType(GroupedOpenApi.class).keySet()).contains("inventory");
        assertThat(fixture.context.getBean("inventory", GroupedOpenApi.class).getOpenApiCustomizers()).isNotEmpty();
        fixture.mvc.perform(get("/v3/api-docs/inventory").header("Authorization", credential("yuheng.openapi.read")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"openapiGroup\":\"inventory\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("orders.list")));
    }

    private String credential(String scope) throws Exception { return "Bearer " + fixture.token(RESOURCE, "at+jwt", "SERVICE", scope); }

    @Slf4j
    @Configuration(value = "docsMvcContractApplication", proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableWebSecurity
    @EnableWebMvc
    @Import(GatewayOpenApiAutoConfiguration.class)
    static class TestApplication {
        @Bean("orders") GroupedOpenApi orders() { return GroupedOpenApi.builder().group("orders").pathsToMatch("/orders").build(); }
        @Bean("orderController") OrderController orderController() { return new OrderController(); }
    }

    @Slf4j
    @RestController("orderController")
    @EgonApiCatalog(businessDomainCode = "trade", entityDomainCode = "order", interfaceGroupCode = "orders")
    static class OrderController {
        @GetMapping("/orders")
        @Operation(operationId = "orders.list")
        Map<String, String> list() { return Map.of("status", "ok"); }
    }
}
