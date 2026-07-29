package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsRulesAsMap() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.access-guard.rules.draw.rate-limit.enabled=true",
                        "egon.cola.component.access-guard.rules.draw.rate-limit.capacity=100",
                        "egon.cola.component.access-guard.rules.draw.rate-limit.refill-tokens=20",
                        "egon.cola.component.access-guard.rules.draw.rate-limit.refill-period=1s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GuardPlanProperties properties = context.getBean(GuardPlanProperties.class);
                    assertThat(properties.getRules()).containsOnlyKeys("draw");
                    assertThat(properties.getRules().get("draw").getRateLimit().getCapacity()).isEqualTo(100L);
                });
    }

    @Test
    void rejectsUnknownFieldsInsteadOfSilentlyIgnoringThem() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.access-guard.rules.draw.rate-limit.enabled=true",
                        "egon.cola.component.access-guard.rules.draw.rate-limit.capacity=100",
                        "egon.cola.component.access-guard.rules.draw.unknown-option=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(GuardPlanProperties.class)
    static class PropertiesConfiguration {
    }
}
