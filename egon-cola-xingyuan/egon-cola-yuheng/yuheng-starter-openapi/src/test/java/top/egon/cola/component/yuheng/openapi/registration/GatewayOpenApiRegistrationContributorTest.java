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
                        "yuheng.definition-source", "OPENAPI31",
                        "yuheng.openapi.enabled", "true",
                        "yuheng.openapi.path-template", "/v3/api-docs/{group}",
                        "yuheng.openapi.spec", "3.1",
                        "yuheng.openapi.groups", "inventory,orders",
                        "yuheng.openapi.resource-uri",
                        "https://order-service.example.test",
                        "yuheng.artifact-version", "1.0.0",
                        "yuheng.build-id", "build-1"
                )
        );
    }
}
