package top.egon.cola.component.tianshu.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAiOptimizationPropertiesTest {

    private static final String PREFIX = DdcAiOptimizationProperties.PREFIX + ".";

    @Test
    void threeProfilesDeclareTheSameAiKeySet() {
        Set<String> base = aiKeys("application.yml");

        assertThat(aiKeys("application-local.yml")).isEqualTo(base);
        assertThat(aiKeys("application-test.yml")).isEqualTo(base);
        assertThat(base).containsExactlyInAnyOrder(
                "optimization.enabled", "optimization.scan-interval", "optimization.run-lease",
                "optimization.max-concurrent-runs", "optimization.sample-limit",
                "judge.scan-interval", "judge.batch-size", "judge.max-attempts", "judge.max-context-bytes", "judge.lease",
                "model.base-url", "model.resource-uri", "model.registration-id", "model.app-id", "model.scopes",
                "model.connect-timeout", "model.read-timeout", "model.max-output-tokens", "model.temperature",
                "feedback.retention", "feedback.retention-scan-interval", "feedback.max-batch",
                "feedback.max-clock-skew", "feedback.max-input-excerpt-bytes", "feedback.max-output-excerpt-bytes");
    }

    @Test
    void testProfileBindsSpecDefaultsWithTheChannelDisabled() {
        DdcAiOptimizationProperties properties = bind("application-test.yml");

        assertThat(properties.getOptimization().isEnabled()).isFalse();
        assertThat(properties.getOptimization().getScanInterval()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getJudge().getMaxAttempts()).isEqualTo(3);
        assertThat(properties.getJudge().getLease()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getModel().getReadTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getModel().getScopes()).containsExactly("yuheng:llm:invoke");
        assertThat(properties.getFeedback().getRetention()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.getFeedback().getMaxOutputExcerptBytes()).isEqualTo(32768);
    }

    @Test
    void runtimeCredentialAllowsUsageReports() {
        Properties profile = load("application.yml");

        assertThat(profile.getProperty("egon.cola.component.tianshu.admin.rpc.credentials[0].allowed-operations"))
                .contains("AI_READ", "AI_REPORT").doesNotContain("AI_WRITE", "AI_PUBLISH");
    }

    private static Set<String> aiKeys(String profile) {
        return load(profile).keySet().stream().map(Object::toString)
                .filter(key -> key.startsWith(PREFIX))
                .map(key -> key.substring(PREFIX.length()).replaceAll("\\[\\d+]$", ""))
                .collect(Collectors.toSet());
    }

    private static DdcAiOptimizationProperties bind(String profile) {
        Map<String, Object> values = load(profile).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
        return new Binder(List.of(new MapConfigurationPropertySource(values)))
                .bind(DdcAiOptimizationProperties.PREFIX, Bindable.of(DdcAiOptimizationProperties.class))
                .orElseThrow(() -> new IllegalStateException("AI properties missing"));
    }

    private static Properties load(String profile) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(profile));
        Properties properties = yaml.getObject();
        assertThat(properties).isNotNull();
        return properties;
    }
}
