package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.test.service.SampleConfigService;
import top.egon.cola.component.rpc.ddc.client.config.RpcDdcConfigClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "egon.cola.component.ddc.enabled=true",
        "egon.cola.component.ddc.app-code=demo-app",
        "egon.cola.component.ddc.env=dev",
        "egon.cola.component.ddc.namespace=default",
        "egon.cola.component.ddc.rpc.target=dns:///127.0.0.1:19080",
        "egon.cola.component.ddc.rpc.tls.development-plaintext=true",
        "egon.cola.component.ddc.rpc.auth.runtime.access-key=test",
        "egon.cola.component.ddc.rpc.auth.runtime.secret-key=test",
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

    @Autowired
    private ObjectProvider<DdcRuntimeCoordinator> runtimeCoordinator;

    @Autowired
    private DdcConfigClient configClient;

    @Test
    void offlineModeKeepsAnnotationDefaultsWithoutRegisteringOrPulling() {
        assertThat(runtimeCoordinator.getIfAvailable()).isNull();
        assertThat(configClient).isInstanceOf(RpcDdcConfigClient.class);
        assertThat(sampleConfigService.getDowngradeSwitch()).isFalse();
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(100);
    }
}
