package top.egon.cola.component.tianshu.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.tianshu.test.service.SampleConfigService;
import top.egon.cola.component.rpc.tianshu.client.config.RpcDdcConfigClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "egon.cola.component.tianshu.enabled=true",
        "egon.cola.component.tianshu.app-code=demo-app",
        "egon.cola.component.tianshu.env=dev",
        "egon.cola.component.tianshu.namespace=default",
        "egon.cola.component.tianshu.rpc.target=dns:///127.0.0.1:19080",
        "egon.cola.component.tianshu.rpc.tls.development-plaintext=true",
        "egon.cola.component.tianshu.rpc.auth.runtime.access-key=test",
        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key=test",
        "egon.cola.component.tianshu.redis.enabled=false",
        "egon.cola.component.tianshu.consistency.fail-fast=false",
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
