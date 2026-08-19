package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ruleengine.autoconfigure.RuleEngineAutoConfiguration;
import top.egon.cola.component.ruleengine.engine.RuleEngine;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineAutoConfigurationSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RuleEngineAutoConfiguration.class));

    @Test
    void shouldInjectRuleEngineFromStarter() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RuleEngine.class));
    }
}
