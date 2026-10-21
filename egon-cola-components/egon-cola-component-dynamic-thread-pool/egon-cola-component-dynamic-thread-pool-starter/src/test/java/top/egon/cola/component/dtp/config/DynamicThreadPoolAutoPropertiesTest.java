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
                    "egon.cola.component.dtp.report.interval=20s"
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
        });
    }

    @Test
    void newDefaultsAndInvalidLogIntervalsAreValidated() {
        contextRunner.run(context -> {
            var properties = context.getBean(DynamicThreadPoolAutoProperties.class);
            assertThat(properties.getContextPropagation().isCustomThreadLocalsEnabled()).isTrue();
            assertThat(properties.getExceptionHandling().isEnabled()).isTrue();
            assertThat(properties.getExceptionHandling().isAsyncVoidEnabled()).isTrue();
            assertThat(properties.getExceptionHandling().isUncaughtEnabled()).isTrue();
            assertThat(properties.getExceptionHandling().getLogInterval()).hasSeconds(1);
        });
        contextRunner.withPropertyValues("egon.cola.component.dtp.exception-handling.log-interval=0s")
                .run(context -> assertThat(context).hasFailed());
        var properties = new DynamicThreadPoolAutoProperties.ExceptionHandling();
        properties.setLogInterval(java.time.Duration.ofSeconds(Long.MAX_VALUE));
        assertThat(properties.isDurationValid()).isFalse();
    }

    @Test
    void allEightApplicationProfilesHaveTheSameNewKeys() {
        var keys = java.util.List.of(
                "context-propagation.custom-thread-locals-enabled", "exception-handling.enabled",
                "exception-handling.log-interval", "exception-handling.uncaught-enabled", "exception-handling.async-void-enabled");
        for (String module : java.util.List.of("admin", "test")) {
            for (String profile : java.util.List.of("", "-dev", "-test", "-prod")) {
                var loader = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
                var path = java.nio.file.Path.of("..", "egon-cola-component-dynamic-thread-pool-" + module,
                        "src", "main", "resources", "application" + profile + ".yml");
                loader.setResources(new org.springframework.core.io.FileSystemResource(path));
                var values = loader.getObject();
                assertThat(values).isNotNull();
                for (String key : keys) {
                    assertThat(values).as(path + " must contain " + key).containsKey("egon.cola.component.dtp." + key);
                }
            }
        }
    }

    @Configuration
    @EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
    static class PropertiesConfiguration {
    }
}
