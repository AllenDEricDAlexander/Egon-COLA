package top.egon.cola.component.ddc.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class DdcRegistryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            DdcRedisAutoConfiguration.class,
                            DdcAutoConfiguration.class,
                            DdcRegistryAutoConfiguration.class
                    ))
                    .withPropertyValues(
                            "egon.cola.component.ddc.enabled=false",
                            "egon.cola.component.ddc.registry.enabled=true"
                    );

    @Test
    void registryCanBeEnabledWhileConfigClientIsDisabled() {
        contextRunner
                .withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> mock(RedissonClient.class)
                )
                .withBean(
                        DdcServiceRegistryClient.class,
                        () -> mock(DdcServiceRegistryClient.class)
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcServiceRegistryClient.class);
                    assertThat(context).hasSingleBean(DdcServiceKeyFactory.class);
                    assertThat(context).doesNotHaveBean(DdcConfigClient.class);
                });
    }

    @Test
    void registryFailsFastWithoutRegistryPort() {
        contextRunner
                .withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> mock(RedissonClient.class)
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessage(
                                    "Required DdcServiceRegistryClient Port is missing; add "
                                            + "top.egon:egon-cola-component-rpc-ddc-adapter"
                            );
                });
    }

    @Test
    void createsSharedDdcClientWhenApplicationClientExists() {
        RedissonClient applicationClient = mock(RedissonClient.class);
        RedissonClient dedicatedClient = mock(RedissonClient.class);

        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(org.mockito.ArgumentMatchers.any(Config.class)))
                    .thenReturn(dedicatedClient);
            contextRunner.withBean(
                            "applicationRedissonClient",
                            RedissonClient.class,
                            () -> applicationClient
                    )
                    .withBean(
                            DdcServiceRegistryClient.class,
                            () -> mock(DdcServiceRegistryClient.class)
                    )
                    .run(context -> {
                        assertThat(context.getBean("ddcRedissonClient"))
                                .isSameAs(dedicatedClient);
                        assertThat(context.getBean("applicationRedissonClient"))
                                .isSameAs(applicationClient);
                    });
        }
    }

    @Test
    void retainsUserProvidedSharedDdcClient() {
        RedissonClient dedicatedClient = mock(RedissonClient.class);

        contextRunner.withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> dedicatedClient
                )
                .withBean(
                        DdcServiceRegistryClient.class,
                        () -> mock(DdcServiceRegistryClient.class)
                )
                .run(context -> assertThat(context.getBean(
                                "ddcRedissonClient"
                        ))
                        .isSameAs(dedicatedClient));
    }

    @Test
    void configClientCanBeEnabledWhileRegistryIsDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRedisAutoConfiguration.class,
                        DdcAutoConfiguration.class,
                        DdcRegistryAutoConfiguration.class
                ))
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=false"
                )
                .withBean(
                        DdcConfigClient.class,
                        () -> mock(DdcConfigClient.class)
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context).doesNotHaveBean(DdcServiceRegistryClient.class);
                });
    }
}
