package top.egon.cola.component.dtp.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ClassName: ApplicationRedisClientConfigPropertiesTest
 * @description: Redis 客户端配置属性绑定测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
class ApplicationRedisClientConfigPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "egon.cola.component.dtp.registry.redis.host=127.0.0.1",
                    "egon.cola.component.dtp.registry.redis.port=6379",
                    "egon.cola.component.dtp.registry.redis.password=pwd",
                    "egon.cola.component.dtp.registry.redis.database=1",
                    "egon.cola.component.dtp.registry.redis.pool-size=10",
                    "egon.cola.component.dtp.registry.redis.min-idle-size=5",
                    "egon.cola.component.dtp.registry.redis.idle-timeout=30000",
                    "egon.cola.component.dtp.registry.redis.connect-timeout=5000",
                    "egon.cola.component.dtp.registry.redis.retry-attempts=3",
                    "egon.cola.component.dtp.registry.redis.retry-interval=1000",
                    "egon.cola.component.dtp.registry.redis.ping-interval=60000",
                    "egon.cola.component.dtp.registry.redis.keep-alive=true"
            );

    private final ApplicationContextRunner prodContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withInitializer(context -> loadYaml("application-prod.yml")
                    .forEach(source -> context.getEnvironment().getPropertySources().addLast(source)));

    @Test
    void shouldBindRedisPropertiesFromDynamicThreadPoolPrefix() {
        contextRunner.run(context -> {
            AdminApplication.RedisClientConfigProperties properties = context.getBean(AdminApplication.RedisClientConfigProperties.class);

            assertThat(properties.getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getPort()).isEqualTo(6379);
            assertThat(properties.getPassword()).isEqualTo("pwd");
            assertThat(properties.getDatabase()).isEqualTo(1);
            assertThat(properties.getPoolSize()).isEqualTo(10);
            assertThat(properties.getMinIdleSize()).isEqualTo(5);
            assertThat(properties.getIdleTimeout()).isEqualTo(30000);
            assertThat(properties.getConnectTimeout()).isEqualTo(5000);
            assertThat(properties.getRetryAttempts()).isEqualTo(3);
            assertThat(properties.getRetryInterval()).isEqualTo(1000);
            assertThat(properties.getPingInterval()).isEqualTo(60000);
            assertThat(properties.isKeepAlive()).isTrue();
        });
    }

    @Test
    void shouldBindProdRedisPropertiesFromProdProfileResource() {
        prodContextRunner.run(context -> {
            AdminApplication.RedisClientConfigProperties properties = context.getBean(AdminApplication.RedisClientConfigProperties.class);

            assertThat(properties.getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getPort()).isEqualTo(6379);
            assertThat(properties.getPassword()).isEmpty();
            assertThat(properties.getPoolSize()).isEqualTo(10);
            assertThat(properties.getMinIdleSize()).isEqualTo(5);
            assertThat(properties.getIdleTimeout()).isEqualTo(30000);
            assertThat(properties.getConnectTimeout()).isEqualTo(5000);
            assertThat(properties.getRetryAttempts()).isEqualTo(3);
            assertThat(properties.getRetryInterval()).isEqualTo(1000);
            assertThat(properties.getPingInterval()).isEqualTo(60000);
            assertThat(properties.isKeepAlive()).isTrue();
        });
    }

    @Test
    void shouldSerializeDtpMessageInstantWithAdminRedisObjectMapper() throws Exception {
        ObjectMapper objectMapper = AdminApplication.RedisClientConfig.createRedisObjectMapper();
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setAppName("order-app");
        message.setTimestamp(Instant.parse("2026-06-30T00:00:00Z"));

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("timestamp");
    }

    @Test
    void shouldExcludeStarterAutoConfigurationFromAdminApplication() {
        assertThat(AdminApplication.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class).exclude())
                .contains(DynamicThreadPoolAutoConfig.class);
    }

    @Test
    void shouldLimitAdminComponentScanToAdminComponents() {
        assertThat(AdminApplication.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class).scanBasePackages())
                .containsExactlyInAnyOrder(
                        "top.egon.cola.component.dtp.admin.config",
                        "top.egon.cola.component.dtp.admin.trigger",
                        "top.egon.cola.component.dtp.admin.manifest"
                );
    }

    @Configuration
    @EnableConfigurationProperties(AdminApplication.RedisClientConfigProperties.class)
    static class PropertiesConfiguration {
    }

    private List<PropertySource<?>> loadYaml(String resourceName) {
        try {
            return new YamlPropertySourceLoader().load(resourceName, new ClassPathResource(resourceName));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourceName, e);
        }
    }

}
