package top.egon.cola.component.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钉死 {@code egon.cola.component.cache} 键树绑定、ISO-8601 时长、默认值与 jakarta 校验边界；
 * 仅启用本 Properties 的绑定（不依赖自动装配）。两级 TTL 各自独立，旧共享键的拒绝合同见
 * {@code RequiredTwoLevelCacheContractTest}。
 */
class EgonColaCachePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void defaultsMatchSpecKeyTree() {
        contextRunner.run(context -> {
            EgonColaCacheProperties props = context.getBean(EgonColaCacheProperties.class);

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getNodeId()).isEmpty();
            assertThat(props.getKeyPrefix()).isEqualTo("egon:cola:cache");
            assertThat(props.getRedis().getTopic()).isEqualTo("egon:cola:cache:event");
            assertThat(props.getTenantMdcKey()).isEqualTo("tenantId");
            assertThat(props.getSecondEvictDelay()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.getL1().getMaxSize()).isEqualTo(10000);
            assertThat(props.getTtl().getL1Expire()).isEqualTo(Duration.ofMinutes(5));
            assertThat(props.getTtl().getL1Jitter()).isEqualTo(Duration.ofMinutes(2));
            assertThat(props.getTtl().getL2Expire()).isEqualTo(Duration.ofHours(1));
            assertThat(props.getTtl().getL2Jitter()).isEqualTo(Duration.ofMinutes(20));
            assertThat(props.getTtl().getNullExpire()).isEqualTo(Duration.ofSeconds(60));
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
                "egon.cola.component.cache.ttl.l1-expire=PT3M",
                "egon.cola.component.cache.ttl.l1-jitter=PT1M",
                "egon.cola.component.cache.ttl.l2-expire=PT2H",
                "egon.cola.component.cache.ttl.l2-jitter=PT10M",
                "egon.cola.component.cache.ttl.null-expire=PT30S",
                "egon.cola.component.cache.lock.wait-time=PT0.5S",
                "egon.cola.component.cache.lock.lease-time=PT10S"
        ).run(context -> {
            EgonColaCacheProperties props = context.getBean(EgonColaCacheProperties.class);

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getNodeId()).isEqualTo("node-a");
            assertThat(props.getSecondEvictDelay()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.getTtl().getL1Expire()).isEqualTo(Duration.ofMinutes(3));
            assertThat(props.getTtl().getL1Jitter()).isEqualTo(Duration.ofMinutes(1));
            assertThat(props.getTtl().getL2Expire()).isEqualTo(Duration.ofHours(2));
            assertThat(props.getTtl().getL2Jitter()).isEqualTo(Duration.ofMinutes(10));
            assertThat(props.getTtl().getNullExpire()).isEqualTo(Duration.ofSeconds(30));
            assertThat(props.getLock().getWaitTime()).isEqualTo(Duration.ofMillis(500));
            assertThat(props.getLock().getLeaseTime()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void bindsPartialRegionTtlOverridesAndInheritsTheRest() {
        contextRunner.withPropertyValues(
                "egon.cola.component.cache.regions.users.l1-expire=PT10S",
                "egon.cola.component.cache.regions.users.l2-jitter=PT30S"
        ).run(context -> {
            var region = context.getBean(EgonColaCacheProperties.class).getRegions().get("users");

            assertThat(region.getL1Expire()).isEqualTo(Duration.ofSeconds(10));
            assertThat(region.getL2Jitter()).isEqualTo(Duration.ofSeconds(30));
            assertThat(region.getL1Jitter()).isNull();
            assertThat(region.getL2Expire()).isNull();
            assertThat(region.getNullExpire()).isNull();
        });
    }

    @Test
    void rejectsNonPositiveExpireAndNegativeJitter() {
        assertRejected("egon.cola.component.cache.ttl.l1-expire=PT0S", "durationValid");
        assertRejected("egon.cola.component.cache.ttl.l2-expire=PT-1S", "durationValid");
        assertRejected("egon.cola.component.cache.ttl.l1-jitter=PT-1S", "durationValid");
        assertRejected("egon.cola.component.cache.ttl.null-expire=PT0S", "durationValid");
        assertRejected("egon.cola.component.cache.regions.users.l1-expire=PT0S", "durationValid");
    }

    @Test
    void ttlGuardRejectsOverflowingAndNonPositiveDurations() {
        EgonColaCacheProperties.Ttl defaults = new EgonColaCacheProperties.Ttl();
        assertThat(defaults.isDurationValid()).isTrue();

        defaults.setL1Expire(Duration.ofSeconds(Long.MAX_VALUE));
        assertThat(defaults.isDurationValid()).isFalse();

        EgonColaCacheProperties.Ttl negativeJitter = new EgonColaCacheProperties.Ttl();
        negativeJitter.setL2Jitter(Duration.ofSeconds(-1));
        assertThat(negativeJitter.isDurationValid()).isFalse();

        EgonColaCacheProperties.Ttl zeroNullExpire = new EgonColaCacheProperties.Ttl();
        zeroNullExpire.setNullExpire(Duration.ZERO);
        assertThat(zeroNullExpire.isDurationValid()).isFalse();
    }

    @Test
    void regionGuardAcceptsPartialOverridesAndRejectsBrokenOnes() {
        assertThat(new EgonColaCacheProperties.RegionTtl().isDurationValid()).isTrue();

        EgonColaCacheProperties.RegionTtl region = new EgonColaCacheProperties.RegionTtl();
        region.setL1Expire(Duration.ofMillis(-1));
        assertThat(region.isDurationValid()).isFalse();
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
            assertThat(rootMessage(failure)).contains(violatedField);
        });
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
