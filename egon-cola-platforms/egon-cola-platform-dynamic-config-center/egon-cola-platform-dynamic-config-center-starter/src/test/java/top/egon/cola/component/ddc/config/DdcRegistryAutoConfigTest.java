package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class DdcRegistryAutoConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            DdcRedisAutoConfig.class,
                            DdcAutoConfig.class,
                            DdcRegistryAutoConfig.class
                    ))
                    .withPropertyValues(
                            "egon.cola.component.ddc.enabled=false",
                            "egon.cola.component.ddc.registry.enabled=true",
                            "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                            "egon.cola.component.ddc.admin.tls."
                                    + "development-plaintext=true"
                    );

    @Test
    void registryCanBeEnabledWhileConfigClientIsDisabled() {
        contextRunner
                .withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> mock(RedissonClient.class)
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcServiceRegistryClient.class);
                    assertThat(context).hasSingleBean(DdcServiceKeyFactory.class);
                    assertThat(context).doesNotHaveBean(DdcAdminClient.class);
                });
    }

    @Test
    void registryFailsBeforeNetworkAccessWhenEndpointIsMissing() {
        contextRunner
                .withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> mock(RedissonClient.class)
                )
                .withPropertyValues("egon.cola.component.ddc.admin.endpoint=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasRootCauseMessage(
                                    "egon.cola.component.ddc.admin.endpoint is required"
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
                .run(context -> assertThat(context.getBean(
                                "ddcRedissonClient"
                        ))
                        .isSameAs(dedicatedClient));
    }

    @Test
    void configClientCanBeEnabledWhileRegistryIsDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRedisAutoConfig.class,
                        DdcAutoConfig.class,
                        DdcRegistryAutoConfig.class
                ))
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=false",
                        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcAdminClient.class);
                    assertThat(context).doesNotHaveBean(DdcServiceRegistryClient.class);
                });
    }
}
