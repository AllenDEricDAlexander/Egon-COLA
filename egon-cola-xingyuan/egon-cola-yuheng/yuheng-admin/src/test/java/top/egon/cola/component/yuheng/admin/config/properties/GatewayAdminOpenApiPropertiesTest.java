package top.egon.cola.component.yuheng.admin.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayAdminOpenApiPropertiesTest {

    @Test
    void exposesTheApprovedBoundedSyncDefaults() {
        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getReconcileDelay())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getBatchSize()).isEqualTo(50);
        assertThat(properties.getMaximumInstanceAttempts()).isEqualTo(3);
        assertThat(properties.getClaimTimeout())
                .isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getConnectTimeout())
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getMaximumDocumentBytes()).isEqualTo(5 * 1024 * 1024);
        assertThat(properties.getRetryInitialDelay())
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getRetryMaximumDelay())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getRetryJitter()).isEqualTo(0.20d);
        assertThat(properties.getDriftSampleInterval())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getRequiredScope())
                .isEqualTo("gateway.openapi.read");
    }

    @Test
    void rejectsEnabledConfigurationWithoutAnAllowlistedCidr() {
        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();
        properties.setEnabled(true);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowed-cidrs");
    }

    @Test
    void normalizesAndDefensivelyCopiesAllowedCidrs() {
        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();
        List<String> cidrs = new java.util.ArrayList<>(List.of("127.0.0.1/32"));

        properties.setAllowedCidrs(cidrs);
        cidrs.add("10.0.0.0/8");

        assertThat(properties.getAllowedCidrs())
                .containsExactly("127.0.0.1/32");
    }

    @Test
    void baseAndLocalProfilesExposeTheSameOpenApiKeyStructure()
            throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        Set<String> base = keys(loader.load(
                "base",
                new ClassPathResource("application.yml")
        ));
        Set<String> local = keys(loader.load(
                "local",
                new ClassPathResource("application-local.yml")
        ));

        assertThat(base).isNotEmpty();
        assertThat(local).containsExactlyInAnyOrderElementsOf(base);
    }

    private Set<String> keys(List<PropertySource<?>> sources) {
        Set<String> keys = new LinkedHashSet<>();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String key : enumerable.getPropertyNames()) {
                    if (key.startsWith("gateway.admin.openapi.")) {
                        keys.add(key.substring(
                                "gateway.admin.openapi.".length()
                        ));
                    }
                }
            }
        }
        return keys;
    }
}
