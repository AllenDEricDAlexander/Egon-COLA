package top.egon.cola.component.common.cache;

import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钉死 {@code egon.cola.component.cache} 13 键绑定、ISO-8601 时长、默认值与
 * jakarta 校验边界；仅启用本 Properties 的绑定（不依赖 Step 6 自动装配）。
 */
class EgonColaCachePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void defaultsMatchSpecKeyTree() {
        contextRunner.run(context -> {
            EgonColaCacheProperties props = context.getBean(EgonColaCacheProperties.class);

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getNodeId()).isEmpty();
            assertThat(props.getKeyPrefix()).isEqualTo("egon:cola:cache");
            assertThat(props.getRedis().getTopic()).isEqualTo("egon:cola:cache:event");
            assertThat(props.getTenantMdcKey()).isEqualTo("tenantId");
            assertThat(props.getSecondEvictDelay()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.getL1().getMaxSize()).isEqualTo(10000);
            assertThat(props.getTtl().getExpire()).isEqualTo(Duration.ofMinutes(30));
            assertThat(props.getTtl().getNullExpire()).isEqualTo(Duration.ofSeconds(60));
            assertThat(props.getTtl().getJitterRatio()).isEqualTo(0.1);
            assertThat(props.getBatch().getMaxKeys()).isEqualTo(1000);
            assertThat(props.getLock().getWaitTime()).isEqualTo(Duration.ofMillis(500));
            assertThat(props.getLock().getLeaseTime()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void bindsIso8601DurationsViaRelaxedKeys() {
        contextRunner.withPropertyValues(
                "egon.cola.component.cache.enabled=true",
                "egon.cola.component.cache.node-id=node-a",
                "egon.cola.component.cache.second-evict-delay=PT5S",
                "egon.cola.component.cache.ttl.expire=PT45M",
                "egon.cola.component.cache.ttl.null-expire=PT30S",
                "egon.cola.component.cache.ttl.jitter-ratio=0.25",
                "egon.cola.component.cache.lock.wait-time=PT0.5S",
                "egon.cola.component.cache.lock.lease-time=PT10S"
        ).run(context -> {
            EgonColaCacheProperties props = context.getBean(EgonColaCacheProperties.class);

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getNodeId()).isEqualTo("node-a");
            assertThat(props.getSecondEvictDelay()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.getTtl().getExpire()).isEqualTo(Duration.ofMinutes(45));
            assertThat(props.getTtl().getNullExpire()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.getTtl().getJitterRatio()).isEqualTo(0.25);
            assertThat(props.getLock().getWaitTime()).isEqualTo(Duration.ofMillis(500));
            assertThat(props.getLock().getLeaseTime()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void rejectsJitterRatioAboveHalf() {
        assertRejected("egon.cola.component.cache.ttl.jitter-ratio=0.6", "jitterRatio");
    }

    @Test
    void rejectsNegativeJitterRatio() {
        assertRejected("egon.cola.component.cache.ttl.jitter-ratio=-0.1", "jitterRatio");
    }

    @Test
    void rejectsNonPositiveL1MaxSize() {
        assertRejected("egon.cola.component.cache.l1.max-size=0", "maxSize");
    }

    private void assertRejected(String property, String violatedField) {
        contextRunner.withPropertyValues(property).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();
            assertThat(hasCause(failure, BindValidationException.class))
                    .as("startup failure caused by bind-time validation of " + violatedField)
                    .isTrue();
            assertThat(rootMessage(failure)).contains(violatedField);
        });
    }

    private static boolean hasCause(Throwable failure, Class<?> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EgonColaCacheProperties.class)
    static class PropertiesConfiguration {
    }
}
