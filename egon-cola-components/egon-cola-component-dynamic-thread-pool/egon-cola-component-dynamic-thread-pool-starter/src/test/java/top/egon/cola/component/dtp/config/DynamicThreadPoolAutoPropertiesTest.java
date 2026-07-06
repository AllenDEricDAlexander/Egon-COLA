package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @description DynamicThreadPoolAutoProperties 属性绑定测试
 */
class DynamicThreadPoolAutoPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "server.port=8093",
                    "egon.cola.component.dtp.enabled=true",
                    "egon.cola.component.dtp.registry.redis.host=127.0.0.1",
                    "egon.cola.component.dtp.registry.redis.port=6379",
                    "egon.cola.component.dtp.registry.redis.password=pwd",
                    "egon.cola.component.dtp.report.interval=20s",
                    "egon.cola.component.dtp.trace.trace-id-key=traceId",
                    "egon.cola.component.dtp.trace.request-id-key=requestId",
                    "egon.cola.component.dtp.virtual.default-concurrency-limit=500"
            );

    @Test
    void shouldBindNewPrefix() {
        contextRunner.run(context -> {
            DynamicThreadPoolAutoProperties properties = context.getBean(DynamicThreadPoolAutoProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getRegistry().getRedis().getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getRegistry().getRedis().getPort()).isEqualTo(6379);
            assertThat(properties.getRegistry().getRedis().getPassword()).isEqualTo("pwd");
            assertThat(properties.getReport().getInterval()).hasSeconds(20);
            assertThat(properties.getTrace().getTraceIdKey()).isEqualTo("traceId");
            assertThat(properties.getTrace().getRequestIdKey()).isEqualTo("requestId");
            assertThat(properties.getVirtual().getDefaultConcurrencyLimit()).isEqualTo(500);
        });
    }

    @Configuration
    @EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
    static class PropertiesConfiguration {
    }
}
