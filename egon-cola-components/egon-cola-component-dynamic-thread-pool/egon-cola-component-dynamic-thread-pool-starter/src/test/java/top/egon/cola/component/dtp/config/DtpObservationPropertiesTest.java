package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

class DtpObservationPropertiesTest {
    @Test
    void allProfilesDeclareTheSameObservationKeysAndProductionDocsStayDisabled() {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !java.nio.file.Files.isDirectory(root.resolve("egon-cola-components"))) { root = root.getParent(); }
        assertThat(root).isNotNull();
        Path dtp = root.resolve("egon-cola-components/egon-cola-component-dynamic-thread-pool");
        Set<String> expected = Set.of("enabled", "slow-task-threshold", "queue-wait-threshold", "alarm.enabled",
                "alarm.queue-usage-threshold", "alarm.consecutive-samples", "alarm.cooldown");
        for (String module : new String[]{"admin", "test"}) {
            for (String suffix : new String[]{"", "-dev", "-test", "-prod"}) {
                var yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(new FileSystemResource(dtp.resolve("egon-cola-component-dynamic-thread-pool-" + module)
                        .resolve("src/main/resources/application" + suffix + ".yml")));
                Properties values = yaml.getObject();
                String prefix = "egon.cola.component.dtp.observation.";
                Set<String> keys = values.keySet().stream().map(Object::toString).filter(key -> key.startsWith(prefix))
                        .map(key -> key.substring(prefix.length())).collect(Collectors.toSet());
                assertThat(keys).as(module + suffix).isEqualTo(expected);
                if (module.equals("admin") && (suffix.isEmpty() || suffix.equals("-prod"))) {
                    assertThat(String.valueOf(values.get("springdoc.api-docs.enabled"))).isEqualTo("false");
                }
            }
        }
    }

    @Test
    void absentNewQueueFieldDoesNotBreakTheLegacyRedisPayload() throws Exception {
        var mapper = DynamicThreadPoolAutoConfig.createRedisObjectMapper();
        var command = new top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand();
        command.setCorePoolSize(2); command.setMaximumPoolSize(4);
        assertThat(mapper.writeValueAsString(command)).doesNotContain("queueCapacity");
        command.setQueueCapacity(64);
        assertThat(mapper.writeValueAsString(command)).contains("\"queueCapacity\":64");
    }

    @Test
    void invalidThresholdsAreActuallyRejectedByTheValidationProvider() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var properties = new DynamicThreadPoolAutoProperties();
            assertThat(validator.validate(properties)).isEmpty();
            properties.getObservation().setSlowTaskThreshold(Duration.ZERO);
            properties.getObservation().getAlarm().setConsecutiveSamples(0);
            assertThat(validator.validate(properties)).hasSize(2);
        }
    }
}
