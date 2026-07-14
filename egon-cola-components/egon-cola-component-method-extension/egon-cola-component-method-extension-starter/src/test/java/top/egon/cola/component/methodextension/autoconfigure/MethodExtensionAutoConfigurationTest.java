package top.egon.cola.component.methodextension.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;
import top.egon.cola.component.methodextension.aop.MethodExtensionAop;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodExtensionAutoConfiguration.class));

    @Test
    void shouldCreateCoreBeansWithoutObjectMapper() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(MethodExtensionProperties.class)
                    .hasSingleBean(MethodExtensionMethodResolver.class)
                    .hasSingleBean(MethodExtensionHandlerResolver.class)
                    .hasSingleBean(MethodExtensionResponseResolver.class)
                    .hasSingleBean(MethodExtensionAop.class);
            MethodExtensionProperties properties = context.getBean(MethodExtensionProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        });
    }

    @Test
    void shouldDisableAutoConfigurationByProperty() {
        contextRunner.withPropertyValues("egon.cola.component.method-extension.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MethodExtensionAop.class));
    }

    @Test
    void shouldBindConfiguredAspectOrder() {
        contextRunner.withPropertyValues("egon.cola.component.method-extension.order=-77")
                .run(context -> {
                    assertThat(context.getBean(MethodExtensionProperties.class).getOrder()).isEqualTo(-77);
                    assertThat(context.getBean(MethodExtensionAop.class).getOrder()).isEqualTo(-77);
                });
    }
}
