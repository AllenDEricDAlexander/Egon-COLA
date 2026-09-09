package top.egon.cola.component.tianshu.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

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
                            "egon.cola.component.tianshu.enabled=false",
                            "egon.cola.component.tianshu.registry.enabled=true"
                    )
                    .withBean(
                            IdpServiceOAuth2Client.class,
                            () -> mock(IdpServiceOAuth2Client.class)
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
                                            + "top.egon:egon-cola-component-rpc-tianshu-adapter"
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
                        "egon.cola.component.tianshu.enabled=true",
                        "egon.cola.component.tianshu.redis.enabled=false",
                        "egon.cola.component.tianshu.registry.enabled=false"
                )
                .withBean(
                        DdcConfigClient.class,
                        () -> mock(DdcConfigClient.class)
                )
                .withBean(
                        IdpServiceOAuth2Client.class,
                        () -> mock(IdpServiceOAuth2Client.class)
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context).doesNotHaveBean(DdcServiceRegistryClient.class);
                });
    }
}
