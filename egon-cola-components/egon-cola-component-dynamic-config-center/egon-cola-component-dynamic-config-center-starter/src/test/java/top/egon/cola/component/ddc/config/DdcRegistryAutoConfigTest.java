package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DdcRegistryAutoConfigTest {

    @Test
    void registryCanBeEnabledWhileConfigClientIsDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DdcAutoConfig.class, DdcRegistryAutoConfig.class))
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=true",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcServiceRegistryClient.class);
                    assertThat(context).doesNotHaveBean(DdcAdminClient.class);
                });
    }

    @Test
    void configClientCanBeEnabledWhileRegistryIsDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DdcAutoConfig.class, DdcRegistryAutoConfig.class))
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=false",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcAdminClient.class);
                    assertThat(context).doesNotHaveBean(DdcServiceRegistryClient.class);
                });
    }
}
