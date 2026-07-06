package top.egon.cola.component.dtp.test.smoke;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtpSampleSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SampleExecutorConfiguration.class)
            .withInitializer(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
                DefaultListableBeanFactory registry = (DefaultListableBeanFactory) beanFactory;
                registry.removeBeanDefinition("dynamicThreadRedissonClient");
                registry.registerSingleton("dynamicThreadRedissonClient", dynamicThreadRedissonClient());
            }))
            .withPropertyValues(
                    "egon.cola.component.dtp.enabled=true",
                    "egon.cola.component.dtp.app-name=dtp-sample",
                    "egon.cola.component.dtp.instance-id=dtp-sample-1",
                    "egon.cola.component.dtp.report.enabled=false"
            )
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(DynamicThreadPoolAutoConfig.class));

    @Test
    void starterRegistersSampleExecutors() {
        contextRunner.run(context -> {
            ManagedExecutorRegistry registry = context.getBean(ManagedExecutorRegistry.class);
            assertTrue(registry.get("samplePlatformExecutor").isPresent());
            assertTrue(registry.get("sampleVirtualExecutor").isPresent());
        });
    }

    @Configuration
    static class SampleExecutorConfiguration {

        @Bean
        ThreadPoolExecutor samplePlatformExecutor() {
            return new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32));
        }

        @Bean
        BoundedVirtualThreadExecutor sampleVirtualExecutor() {
            return new BoundedVirtualThreadExecutor("sample-virtual", 16);
        }
    }

    private static RedissonClient dynamicThreadRedissonClient() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getTopic(anyString())).thenReturn(mock(RTopic.class));
        return redissonClient;
    }
}
