package top.egon.cola.component.tianshu.admin.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DdcProfileParityTest {

    private static final String OUTBOX_PREFIX = "egon.cola.component.transactional-outbox.";
    private static final List<String> ADMIN_PROFILES = List.of(
            "application.yml", "application-local.yml", "application-test.yml");

    @Test
    void transactionalOutboxKeySetMatchesAcrossProfilesAndTestPollingIsDisabled() {
        Map<String, Properties> profiles = ADMIN_PROFILES.stream()
                .collect(Collectors.toMap(profile -> profile, this::load));
        Set<String> expectedKeys = keys(profiles.get("application.yml"));

        assertThat(keys(profiles.get("application-local.yml"))).isEqualTo(expectedKeys);
        assertThat(keys(profiles.get("application-test.yml"))).isEqualTo(expectedKeys);
        assertThat(profiles.get("application.yml").getProperty(OUTBOX_PREFIX + "enabled")).isEqualTo("true");
        assertThat(profiles.get("application-local.yml").getProperty(OUTBOX_PREFIX + "polling.enabled"))
                .isEqualTo("true");
        assertThat(profiles.get("application-test.yml").getProperty(OUTBOX_PREFIX + "polling.enabled"))
                .isEqualTo("false");
        assertThat(profiles.get("application.yml").getProperty(OUTBOX_PREFIX + "payload.max-bytes"))
                .isEqualTo("8MB");
        assertThat(profiles.get("application.yml").getProperty(OUTBOX_PREFIX + "retry.max-attempts"))
                .isEqualTo("10");
        assertThat(profiles.get("application.yml").getProperty(OUTBOX_PREFIX + "delivery.lease-duration"))
                .isEqualTo("60s");
    }

    private Properties load(String profile) {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource(profile));
        Properties properties = loader.getObject();
        if (properties == null) {
            throw new IllegalStateException("Cannot load Tianshu profile " + profile);
        }
        return properties;
    }

    private Set<String> keys(Properties properties) {
        return properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(OUTBOX_PREFIX))
                .collect(Collectors.toSet());
    }
}
