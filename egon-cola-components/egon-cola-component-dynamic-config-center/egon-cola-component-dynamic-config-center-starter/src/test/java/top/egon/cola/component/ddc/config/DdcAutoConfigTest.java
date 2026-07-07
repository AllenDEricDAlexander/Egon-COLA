package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdcAutoConfig.class));

    @Test
    void doesNotCreateBeansWhenDisabled() {
        contextRunner.withPropertyValues("egon.cola.component.ddc.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DdcFieldBindingService.class));
    }

    @Test
    void createsCoreBeansWhenEnabled() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.app-code=demo",
                        "egon.cola.component.ddc.env=dev",
                        "egon.cola.component.ddc.namespace=default")
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcProperties.class);
                    assertThat(context).hasSingleBean(DdcFieldBindingService.class);
                    assertThat(context).hasSingleBean(DdcAdminClient.class);
                });
    }
}
