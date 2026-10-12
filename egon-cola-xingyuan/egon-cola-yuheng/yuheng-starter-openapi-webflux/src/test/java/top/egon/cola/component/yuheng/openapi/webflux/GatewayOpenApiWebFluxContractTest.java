package top.egon.cola.component.yuheng.openapi.webflux;

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
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springdoc.core.models.GroupedOpenApi;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiWebFluxContractTest {
    private static final URI RESOURCE = URI.create("https://inventory-service.example.test");
    @TempDir Path temporary;
    private final GatewayOpenApiWebFluxSecurityAutoConfigurationTest fixture = new GatewayOpenApiWebFluxSecurityAutoConfigurationTest();

    @BeforeEach
    void setup() throws Exception {
        fixture.temporary = temporary;
        fixture.setupPipeline(RESOURCE, Map.ofEntries(
                Map.entry("egon.cola.component.yuheng.openapi.enabled", true),
                Map.entry("egon.cola.component.yuheng.openapi.biz-code", "warehouse"),
                Map.entry("egon.cola.component.yuheng.openapi.application-code", "inventory-service"),
                Map.entry("egon.cola.component.yuheng.openapi.resource-uri", RESOURCE.toASCIIString()),
                Map.entry("egon.cola.component.yuheng.openapi.artifact-version", "1.0.0"),
                Map.entry("egon.cola.component.yuheng.openapi.build-id", "build-1"),
                Map.entry("egon.cola.component.yuheng.openapi.published-groups[0]", "inventory"),
                Map.entry("egon.cola.component.id.enabled", false),
                Map.entry("spring.autoconfigure.exclude", "org.redisson.spring.starter.RedissonAutoConfigurationV2,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration")
        ), TestApplication.class);
    }

    @AfterEach
    void cleanup() { fixture.cleanup(); }

    @Test
    void servesAGroupedOpenApi31DocumentOnlyForTheRequiredScope() throws Exception {
        fixture.client.get().uri("/v3/api-docs/inventory").header("Authorization", credential("yuheng.openapi.read"))
                .exchange().expectStatus().isOk().expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class).value(body -> { assertThat(body).contains("\"openapi\":\"3.1."); assertThat(body).contains("inventory.list"); });
    }

    @Test
    void deniesAnonymousGroupedDocumentRequests() {
        fixture.client.get().uri("/v3/api-docs/inventory").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deniesJwtWithoutTheRequiredScope() throws Exception {
        fixture.client.get().uri("/v3/api-docs/inventory").header("Authorization", credential("yuheng.other"))
                .exchange().expectStatus().isForbidden();
    }

    private String credential(String scope) throws Exception { return "Bearer " + fixture.token(RESOURCE, "at+jwt", "SERVICE", scope); }

    @Slf4j
    @Configuration(value = "docsFluxContractApplication", proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableWebFluxSecurity
    @EnableWebFlux
    @Import(GatewayOpenApiAutoConfiguration.class)
    static class TestApplication {
        @Bean("inventory") GroupedOpenApi inventory() { return GroupedOpenApi.builder().group("inventory").pathsToMatch("/inventory").build(); }
        @Bean("inventoryController") InventoryController inventoryController() { return new InventoryController(); }
    }

    @Slf4j
    @RestController("inventoryController")
    @EgonApiCatalog(businessDomainCode = "warehouse", entityDomainCode = "inventory", interfaceGroupCode = "inventory")
    static class InventoryController {
        @GetMapping("/inventory")
        @Operation(operationId = "inventory.list")
        Mono<Map<String, String>> list() { return Mono.just(Map.of("status", "ok")); }
    }
}
