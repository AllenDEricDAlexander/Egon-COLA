package top.egon.cola.platform.rbac3.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.field.Rbac3FieldJacksonModule;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Rbac3StarterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Rbac3StarterAutoConfiguration.class));

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuthorizationService.class));
    }

    @Test
    void isOptInAndBacksOffForConsumerAuthorizationService() {
        AuthorizationService consumer = mock(AuthorizationService.class);
        runner.withPropertyValues("egon.cola.platform.rbac3.enabled=true")
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(SensitiveStrategyRegistry.class,
                        SensitiveStrategyRegistry::defaults)
                .withBean(AuthorizationService.class, () -> consumer)
                .run(context -> {
                    assertThat(context.getBean(AuthorizationService.class))
                            .isSameAs(consumer);
                    assertThat(context)
                            .hasSingleBean(AuthorizationManagerBeforeMethodInterceptor.class)
                            .hasSingleBean(Rbac3FieldJacksonModule.class)
                            .doesNotHaveBean("rbac3MethodAuthorizationAspect");
                });
    }
}
