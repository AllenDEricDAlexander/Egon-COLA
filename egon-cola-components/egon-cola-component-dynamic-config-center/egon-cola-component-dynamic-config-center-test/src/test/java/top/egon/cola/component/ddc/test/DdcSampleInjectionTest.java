package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "egon.cola.component.ddc.enabled=true",
        "egon.cola.component.ddc.app-code=demo-app",
        "egon.cola.component.ddc.env=dev",
        "egon.cola.component.ddc.namespace=default",
        "egon.cola.component.ddc.redis.enabled=false",
        "egon.cola.component.ddc.consistency.fail-fast=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
class DdcSampleInjectionTest {

    @Autowired
    private SampleConfigService sampleConfigService;

    @Test
    void sampleBeanKeepsAnnotationDefaultsWhenAdminUnavailableAndFailFastDisabled() {
        assertThat(sampleConfigService.getDowngradeSwitch()).isFalse();
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(100);
    }
}
