package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.executor.ThreadManage;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

class DtpExecutorBeanPostProcessorTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.class))
                .withUserConfiguration(FactoryConfiguration.class)
                .withPropertyValues("egon.cola.component.dtp.report.enabled=false", "spring.application.name=app", "egon.cola.component.dtp.observation.slow-task-threshold=2s")
                .withInitializer(context -> context.addBeanFactoryPostProcessor(factory -> {
                    var definitions = (DefaultListableBeanFactory) factory;
                    definitions.removeBeanDefinition("dynamicThreadRedissonClient");
                    RedissonClient redis = mock(RedissonClient.class);
                    when(redis.getTopic(anyString())).thenReturn(mock(RTopic.class));
                    definitions.registerSingleton("dynamicThreadRedissonClient", redis);
                }));
    }

    @Test
    void factoryBeansAndAliasesRegisterWithoutCircularCreationOrReplacement() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            ThreadPoolTaskExecutor pool = context.getBean("factoryPool", ThreadPoolTaskExecutor.class);
            var registry = context.getBean(ManagedExecutorRegistry.class);
            assertThat(context.getBean("aliasPool")).isSameAs(pool);
            assertThat(registry.get("factoryPool")).isPresent();
            assertThat(registry.get("aliasPool")).containsSame(registry.get("factoryPool").orElseThrow());
            assertThat(registry.list()).hasSize(1);
            assertThat(context.getBean(DynamicThreadPoolAutoProperties.class).getObservation().getSlowTaskThreshold())
                    .isEqualTo(java.time.Duration.ofSeconds(2));
            var nativePool = pool.getThreadPoolExecutor();
            pool.afterPropertiesSet();
            assertThat(pool.getThreadPoolExecutor()).isSameAs(nativePool);
            assertThat(context.getBean(ThreadManage.class).executor("factoryPool").submit(() -> 42).get(2, TimeUnit.SECONDS)).isEqualTo(42);
        });
    }

    @Test
    void micrometerIsOptionalAtRuntime() {
        runner().withClassLoader(new FilteredClassLoader("io.micrometer")).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ThreadManage.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class FactoryConfiguration {
        @Bean({"factoryPool", "aliasPool"})
        ThreadPoolTaskExecutor factoryPool(ThreadManage manager) {
            return manager.newThreadPool(new ThreadPoolCreateCommand().setExecutorName("factoryPool")
                    .setCorePoolSize(1).setMaximumPoolSize(2).setQueueCapacity(4));
        }
    }
}
