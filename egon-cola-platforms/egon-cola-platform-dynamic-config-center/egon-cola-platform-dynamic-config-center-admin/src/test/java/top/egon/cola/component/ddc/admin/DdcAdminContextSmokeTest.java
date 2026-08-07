package top.egon.cola.component.ddc.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the shipped {@code application.yml} against placeholders that no property source
 * can resolve.
 *
 * <p>The Actuator info version is the load-bearing case: {@code info.app.version: ${sdk.version}}
 * only resolves because {@code application.yml} declares
 * {@code spring.config.import: classpath:META-INF/egon-cola-ddc.properties}, and that file is
 * Maven-filtered in the starter module. Drop the import, rename the key, or lose the filtering
 * and the admin stops starting — with no other test in this module noticing, since they all
 * either override the property or use a {@code @WebMvcTest} slice.
 *
 * <p>Scope: this loads Boot's configuration machinery against the real YAML, not the full bean
 * graph — the admin's Redis-backed beans are mandatory and cannot start without a live Redis.
 * {@code Holder} is deliberately un-annotated so that component scanning cannot pick it up and
 * so that it is not a second {@code @SpringBootConfiguration} candidate for the other tests.
 */
@SpringBootTest(classes = DdcAdminContextSmokeTest.Holder.class)
@ActiveProfiles("test")
@Import({InfoContributorAutoConfiguration.class, InfoEndpointAutoConfiguration.class})
class DdcAdminContextSmokeTest {

    static class Holder {
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private InfoEndpoint infoEndpoint;

    @Test
    void actuatorInfoVersionResolvesToAConcreteValue() {
        String version = environment.getProperty(
                "info.app.version"
        );

        assertThat(version)
                .as("Actuator info version must be supplied by the build, not left as a placeholder")
                .isNotBlank()
                .doesNotContain("${")
                .doesNotContain("@");
        assertThat(environment.getProperty("management.info.env.enabled", Boolean.class))
                .as("Actuator environment info contributor must expose info.app.version")
                .isTrue();
        assertThat(infoEndpoint.info())
                .containsEntry("app", Map.of(
                        "name", "egon-cola-ddc-admin",
                        "version", version
                ));
    }

    @Test
    void everyShippedPropertyResolves() {
        List<String> unresolved = new ArrayList<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)
                    || !source.getName().contains("application")) {
                continue;
            }
            for (String key : enumerable.getPropertyNames()) {
                try {
                    String value = environment.getProperty(key);
                    if (value != null && value.contains("${")) {
                        unresolved.add(key + " -> " + value);
                    }
                } catch (IllegalArgumentException ex) {
                    unresolved.add(key + " -> " + ex.getMessage());
                }
            }
        }

        assertThat(unresolved)
                .as("shipped configuration must not contain unresolvable placeholders")
                .isEmpty();
    }
}
