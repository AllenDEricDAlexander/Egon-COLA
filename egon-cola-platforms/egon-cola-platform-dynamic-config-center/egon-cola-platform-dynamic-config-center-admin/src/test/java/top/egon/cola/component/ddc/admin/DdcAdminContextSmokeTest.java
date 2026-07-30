package top.egon.cola.component.ddc.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the shipped {@code application.yml} against placeholders that no property source
 * can resolve.
 *
 * <p>The manifest version is the load-bearing case: {@code manifest.version: ${sdk.version}}
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
class DdcAdminContextSmokeTest {

    static class Holder {
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void manifestVersionResolvesToAConcreteValue() {
        String version = environment.getProperty(
                "egon.cola.component.ddc.admin.manifest.version"
        );

        assertThat(version)
                .as("manifest version must be supplied by the build, not left as a placeholder")
                .isNotBlank()
                .doesNotContain("${")
                .doesNotContain("@");
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
