package top.egon.cola.component.yuheng.openapi.registration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiRegistrationContributorTest {

    @Test
    void publishesOnlyTheBoundedOpenApiMetadataContract() {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setEnabled(true);
        properties.setPublishedGroups(List.of("orders", "inventory"));
        properties.setBizCode("trade");
        properties.setApplicationCode("order-service");
        properties.setResourceUri("https://order-service.example.test");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.validate();

        GatewayOpenApiRegistrationContributor contributor =
                new GatewayOpenApiRegistrationContributor(properties);

        assertThat(contributor.serviceVersion()).isEqualTo("1.0.0");
        assertThat(contributor.metadata()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "gateway.definition-source", "OPENAPI31",
                        "gateway.openapi.enabled", "true",
                        "gateway.openapi.path-template", "/v3/api-docs/{group}",
                        "gateway.openapi.spec", "3.1",
                        "gateway.openapi.groups", "inventory,orders",
                        "gateway.openapi.resource-uri",
                        "https://order-service.example.test",
                        "gateway.artifact-version", "1.0.0",
                        "gateway.build-id", "build-1"
                )
        );
    }
}
