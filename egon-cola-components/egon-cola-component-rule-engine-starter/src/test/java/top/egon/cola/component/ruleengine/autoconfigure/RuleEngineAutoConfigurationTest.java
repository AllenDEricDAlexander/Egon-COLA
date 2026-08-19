package top.egon.cola.component.ruleengine.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;
import top.egon.cola.component.ruleengine.engine.RuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.engine.RuleTreeExecutor;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RuleEngineAutoConfiguration.class));

    @Test
    void shouldCreateDefaultBeansWhenEnabled() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(RuleEngine.class)
                .hasSingleBean(RuleChainExecutor.class)
                .hasSingleBean(RuleTreeExecutor.class)
                .hasSingleBean(RuleAsyncExecutor.class)
                .hasSingleBean(RuleExecutionListenerComposite.class));
    }

    @Test
    void shouldDisableAutoConfigurationByProperty() {
        contextRunner.withPropertyValues("egon.cola.component.rule-engine.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RuleEngine.class));
    }
}
