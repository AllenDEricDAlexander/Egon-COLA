package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.config.AccessGuardConfigProvider;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessGuardAutoConfiguration.class));

    @Test
    void shouldCreateCoreBeansWhenEnabled() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(AccessGuardProperties.class)
                .hasSingleBean(AccessGuardConfigProvider.class)
                .hasSingleBean(AccessGuardEventPublisher.class)
                .hasSingleBean(AccessGuardAop.class));
    }

    @Test
    void shouldNotCreateAopWhenDisabled() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AccessGuardAop.class));
    }

    @Test
    void shouldSelectAgentWithoutCreatingAop() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.engine=AGENT")
                .run(context -> assertThat(context)
                        .hasSingleBean(AccessGuardExecutionService.class)
                        .doesNotHaveBean(AccessGuardAop.class));
    }

    @Test
    void shouldDisableIntegrationWhenEngineIsDisabled() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.engine=DISABLED")
                .run(context -> assertThat(context).doesNotHaveBean(AccessGuardAop.class));
    }

    @Test
    void shouldRejectMethodOnlyKeyResolverForAgentConstructorSupport() {
        AccessKeyResolver methodOnlyResolver = (joinPoint, rule) -> null;

        contextRunner.withPropertyValues("egon.cola.component.access-guard.engine=AGENT")
                .withBean(AccessKeyResolver.class, () -> methodOnlyResolver)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("ExecutableAccessKeyResolver"));
    }
}
