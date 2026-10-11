package top.egon.cola.platform.tianquan.jianshen.yuheng.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.yuheng.core.security.GatewayIdentityMapper;
import top.egon.cola.platform.tianquan.jianshen.yuheng.runtime.Rbac3GatewayScopeSnapshotReader;
import top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3BizAppScopeAuthorizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Rbac3GatewayAdapterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Rbac3GatewayAdapterAutoConfiguration.class))
            .withBean(ObjectMapper.class,
                    () -> new ObjectMapper().findAndRegisterModules());

    @Test
    void staysDisabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GatewayCredentialExtractor.class);
            assertThat(context).doesNotHaveBean(GatewayIdentityMapper.class);
        });
    }

    @Test
    void registersAuthorizationOnlyCapabilityWhenRuntimeIsAvailable() {
        runner.withPropertyValues(
                        "egon.cola.platform.tianquan.jianshen.yuheng.enabled=true")
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(GatewayAuthorizationProvider.class);
                    assertThat(context).hasSingleBean(
                            Rbac3GatewayScopeSnapshotReader.class);
                    assertThat(context).hasSingleBean(
                            Rbac3BizAppScopeAuthorizationProvider.class);
                    assertThat(context).doesNotHaveBean(GatewayCredentialExtractor.class);
                    assertThat(context).doesNotHaveBean(GatewayIdentityMapper.class);
                    assertThat(context).doesNotHaveBean(
                            "rbac3GatewayRuntimeSnapshotReader");
                    assertThat(context).doesNotHaveBean(
                            "rbac3PermissionAuthorizationProvider");
                    assertThat(context.getBean(GatewayAuthorizationProvider.class)
                            .providerId()).isEqualTo("tianquan-jianshen-biz-app-scope");
                });
    }

    @Test
    void configuredProviderReceivesTheExistingObserverMetricsAndValidator() {
        var observer = mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
        top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder metrics = sample -> { };
        var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        runner.withPropertyValues("egon.cola.platform.tianquan.jianshen.yuheng.enabled=true")
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("idpObservedVerificationComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class, () -> observer)
                .withBean("idpSecurityMetricsRecorder", top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class, () -> metrics)
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Rbac3BizAppScopeAuthorizationProvider.class);
                    var provider = context.getBean(Rbac3BizAppScopeAuthorizationProvider.class);
                    for (var dependency : java.util.Map.of("observations", observer, "metrics", metrics, "validator", validator).entrySet()) {
                        try {
                            var field = provider.getClass().getDeclaredField(dependency.getKey()); field.setAccessible(true);
                            assertThat(field.get(provider)).isSameAs(dependency.getValue());
                        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
                    }
                    assertThat(provider.mandatory()).isFalse();
                });
        validator.close();
    }
}
