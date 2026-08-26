package top.egon.cola.component.gateway.openapi.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.openapi.config.GatewayOpenApiProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EgonOpenApiCustomizerTest {

    @Test
    void writesTheBuildAndGroupIdentityToTheRootExtension() {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setPublishedGroups(List.of("orders", "inventory"));
        properties.setBizCode("trade");
        properties.setApplicationCode("order-service");
        properties.setResourceUri("https://order-service.example.test");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");

        OpenAPI openAPI = new OpenAPI();
        new EgonOpenApiCustomizer(properties).customise(openAPI, "orders");

        Map<String, Object> extension = (Map<String, Object>) openAPI.getExtensions()
                .get("x-egon-service");
        assertThat(extension).containsEntry("version", 1);
        assertThat(extension).containsEntry("bizCode", "trade");
        assertThat(extension).containsEntry("applicationCode", "order-service");
        assertThat(extension).containsEntry("artifactVersion", "1.0.0");
        assertThat(extension).containsEntry("buildId", "build-1");
        assertThat(extension).containsEntry("openapiGroup", "orders");
    }
}
