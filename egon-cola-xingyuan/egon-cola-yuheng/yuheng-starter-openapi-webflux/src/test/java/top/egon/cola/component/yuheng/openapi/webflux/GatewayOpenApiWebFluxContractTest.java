package top.egon.cola.component.yuheng.openapi.webflux;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.models.GroupedOpenApi;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = GatewayOpenApiWebFluxContractTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "egon.cola.component.gateway.openapi.enabled=true",
                "egon.cola.component.gateway.openapi.biz-code=warehouse",
                "egon.cola.component.gateway.openapi.application-code=inventory-service",
                "egon.cola.component.gateway.openapi.resource-uri=https://inventory-service.example.test",
                "egon.cola.component.gateway.openapi.artifact-version=1.0.0",
                "egon.cola.component.gateway.openapi.build-id=build-1",
                "egon.cola.component.gateway.openapi.published-groups[0]=inventory",
                "egon.cola.component.id.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
        }
)
@AutoConfigureWebTestClient
class GatewayOpenApiWebFluxContractTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void servesAGroupedOpenApi31DocumentOnlyForTheRequiredScope() {
        webTestClient.get()
                .uri("/v3/api-docs/inventory")
                .header("Authorization", "Bearer allowed")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"openapi\":\"3.1.");
                    assertThat(body).contains("inventory.list");
                });
    }

    @Test
    void deniesAnonymousGroupedDocumentRequests() {
        webTestClient.get()
                .uri("/v3/api-docs/inventory")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deniesJwtWithoutTheRequiredScope() {
        webTestClient.get()
                .uri("/v3/api-docs/inventory")
                .header("Authorization", "Bearer wrong")
                .exchange()
                .expectStatus().isForbidden();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            GatewayOpenApiAutoConfiguration.class,
            GatewayOpenApiWebFluxSecurityAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("scope", "wrong".equals(token)
                            ? "gateway.other"
                            : "gateway.openapi.read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build());
        }

        @Bean
        GroupedOpenApi inventory() {
            return GroupedOpenApi.builder()
                    .group("inventory")
                    .pathsToMatch("/inventory")
                    .build();
        }

        @Bean
        InventoryController inventoryController() {
            return new InventoryController();
        }
    }

    @RestController
    @EgonApiCatalog(
            businessDomainCode = "warehouse",
            entityDomainCode = "inventory",
            interfaceGroupCode = "inventory"
    )
    static class InventoryController {

        @GetMapping("/inventory")
        @Operation(operationId = "inventory.list")
        Mono<Map<String, String>> list() {
            return Mono.just(Map.of("status", "ok"));
        }
    }
}
