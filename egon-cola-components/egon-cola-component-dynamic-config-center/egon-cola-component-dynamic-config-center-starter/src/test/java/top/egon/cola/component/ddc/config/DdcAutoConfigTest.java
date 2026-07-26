package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DdcAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdcAutoConfig.class));

    private final ApplicationContextRunner redisContextRunner =
            new ApplicationContextRunner(NonStartingApplicationContext::new)
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
                        "egon.cola.component.ddc.namespace=default",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcProperties.class);
                    assertThat(context).hasSingleBean(DdcFieldBindingService.class);
                    assertThat(context).hasSingleBean(DdcConfigApplierRegistry.class);
                    assertThat(context).hasSingleBean(DdcAdminClient.class);
                    assertThat(context.getBean(DefaultDdcConfigApplierRegistry.class).frozen()).isTrue();
                });
    }

    @Test
    void createsDedicatedRedisClientWhenApplicationClientExists() {
        RedissonClient applicationClient = mock(RedissonClient.class);
        RedissonClient dedicatedClient = dedicatedClient();

        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(org.mockito.ArgumentMatchers.any(Config.class)))
                    .thenReturn(dedicatedClient);
            redisContextRunner.withBean(
                            "applicationRedissonClient",
                            RedissonClient.class,
                            () -> applicationClient
                    )
                    .withPropertyValues(
                            "egon.cola.component.ddc.redis.enabled=true",
                            "egon.cola.component.ddc.admin.tls."
                                    + "development-plaintext=true"
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
    void retainsUserProvidedDedicatedRedisClient() {
        RedissonClient dedicatedClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(dedicatedClient.getTopic(anyString())).thenReturn(topic);

        redisContextRunner.withBean(
                        "ddcRedissonClient",
                        RedissonClient.class,
                        () -> dedicatedClient
                )
                .withPropertyValues(
                        "egon.cola.component.ddc.redis.enabled=true",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true"
                )
                .run(context -> assertThat(context.getBean("ddcRedissonClient"))
                        .isSameAs(dedicatedClient));
    }

    private RedissonClient dedicatedClient() {
        RedissonClient client = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(client.getTopic(anyString())).thenReturn(topic);
        return client;
    }

    private static final class NonStartingApplicationContext
            extends AnnotationConfigApplicationContext {

        @Override
        protected void finishRefresh() {
        }
    }
}
