package top.egon.cola.component.yuheng.openapi.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOpenApiPropertiesTest {

    @Test
    void validatesAndNormalizesAnEnabledProviderConfiguration() {
        GatewayOpenApiProperties properties = enabledProperties();
        properties.setPublishedGroups(List.of("orders", "inventory"));

        properties.validate();

        assertThat(properties.getPublishedGroups())
                .containsExactly("inventory", "orders");
    }

    @Test
    void rejectsDuplicateUppercaseAndOutOfRangeGroups() {
        GatewayOpenApiProperties properties = enabledProperties();
        properties.setPublishedGroups(List.of("orders", "orders"));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class);

        properties.setPublishedGroups(List.of("Orders"));
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class);

        properties.setPublishedGroups(java.util.stream.IntStream.range(0, 17)
                .mapToObj(index -> "group-" + index)
                .toList());
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GatewayOpenApiProperties enabledProperties() {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setEnabled(true);
        properties.setBizCode("trade");
        properties.setApplicationCode("order-service");
        properties.setResourceUri("https://order-service.example.test");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setPublishedGroups(List.of("orders"));
        return properties;
    }
}
