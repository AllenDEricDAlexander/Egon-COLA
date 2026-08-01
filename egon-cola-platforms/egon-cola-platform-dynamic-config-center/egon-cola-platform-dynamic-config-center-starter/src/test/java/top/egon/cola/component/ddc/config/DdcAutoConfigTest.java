package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcAckDelivery;
import top.egon.cola.component.ddc.service.DdcAckDeliveryProperties;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.DdcFieldBindingService;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
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
    void doesNotCreateBeansWhenEnableFlagIsMissing() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DdcAdminClient.class));
    }

    @Test
    void warnsWhenRemoteLifecycleIsDisabled(CapturedOutput output) {
        contextRunner.withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                        "egon.cola.component.ddc.admin.tls.development-plaintext=true"
                )
                .run(context -> assertThat(output).contains(
                        "DDC remote lifecycle is disabled because "
                                + "egon.cola.component.ddc.redis.enabled=false; "
                                + "no registration, pull, subscription, heartbeat, or ACK will run"
                ));
    }

    @Test
    void createsCoreBeansWhenEnabled() {
        AtomicReference<DdcAckDelivery> delivery = new AtomicReference<>();
        contextRunner.withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.app-code=demo",
                        "egon.cola.component.ddc.env=dev",
                        "egon.cola.component.ddc.namespace=default",
                        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcProperties.class);
                    assertThat(context).hasSingleBean(DdcAckDeliveryProperties.class);
                    assertThat(context).hasSingleBean(DdcAckDelivery.class);
                    assertThat(context).hasSingleBean(DdcFieldBindingService.class);
                    assertThat(context).hasSingleBean(DdcConfigApplierRegistry.class);
                    assertThat(context).hasSingleBean(DdcAdminClient.class);
                    assertThat(context.getBean(DdcAckDelivery.class).isRunning())
                            .isTrue();
                    delivery.set(context.getBean(DdcAckDelivery.class));
                    assertThat(context.getBean(DefaultDdcConfigApplierRegistry.class).frozen()).isTrue();
                });
        assertThat(delivery.get().isRunning()).isFalse();
        assertThat(delivery.get().isWorkerTerminated()).isTrue();
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
                            "egon.cola.component.ddc.enabled=true",
                            "egon.cola.component.ddc.redis.enabled=true",
                            "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
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
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=true",
                        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                        "egon.cola.component.ddc.admin.tls."
                                + "development-plaintext=true"
                )
                .run(context -> assertThat(context.getBean("ddcRedissonClient"))
                        .isSameAs(dedicatedClient));
    }

    @Test
    void retainsUserProvidedAdminClient() {
        DdcAdminClient client = mock(DdcAdminClient.class);

        contextRunner
                .withBean(DdcAdminClient.class, () -> client)
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.admin.endpoint=http://ddc.test"
                )
                .run(context -> assertThat(context.getBean(DdcAdminClient.class))
                        .isSameAs(client));
    }

    @Test
    void subscribesOnlyToPhysicalV3Topic() {
        RedissonClient client = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setEnv("dev");
        properties.setAppCode("order");
        properties.setNamespace("namespace-a");
        when(client.getTopic(DdcKeys.v3Topic("retail", "dev", "order")))
                .thenReturn(topic);

        RTopic result = new DdcAutoConfig().ddcRedisTopic(client, properties);

        assertThat(result).isSameAs(topic);
        verify(client).getTopic(DdcKeys.v3Topic("retail", "dev", "order"));
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
