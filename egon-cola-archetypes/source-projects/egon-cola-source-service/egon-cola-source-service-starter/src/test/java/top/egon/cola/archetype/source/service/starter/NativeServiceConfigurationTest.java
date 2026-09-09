package top.egon.cola.archetype.source.service.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.evaluation.facade.rpc.CourseRpcService;
import top.egon.cola.evaluation.facade.rpc.EvaluationRpcConverter;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = EvaluationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NativeServiceConfigurationTest {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private Environment environment;

    @Test
    void assembles_named_native_providers_and_converters_without_external_lifecycles() {
        assertThat(context.getBean(CourseRpcService.class)).isNotNull();
        assertThat(context.getBean(EvaluationRpcConverter.class)).isNotNull();
        assertThat(context.getBean("nativeRpcValidation", ValidationUtils.class))
                .isSameAs(context.getBean("egonColaValidationUtils"));
        assertThat(context.containsBean("rpcProviderLifecycle")).isFalse();
        assertThat(context.containsBean("ddcRuntimeCoordinator")).isFalse();
        assertThat(context.containsBean("ddcHttpRegistrationRuntime")).isFalse();
        assertThat(context.getBeansOfType(org.redisson.api.RedissonClient.class)).isEmpty();
        for (String suffix : new String[]{"rpc.enabled", "rpc.provider.enabled", "rpc.consumer.enabled",
                "tianshu.enabled", "tianshu.registry.enabled", "tianshu.redis.enabled", "tianshu.registry.http.enabled"}) {
            assertThat(environment.getProperty("egon.cola.component." + suffix)).as(suffix).isEqualTo("false");
        }
        assertThat(environment.getProperty("egon.cola.platform.tianquan.shoubing.enabled")).isEqualTo("false");
    }

    @Test
    void every_profile_uses_the_same_native_and_target_keys() {
        var expected = keys(properties("application.yml"));
        for (String profile : new String[]{"dev", "test", "prod"}) {
            assertThat(keys(properties("application-" + profile + ".yml"))).as(profile).isEqualTo(expected);
        }
        assertThat(properties("application-prod.yml").getProperty("egon.cola.component.rpc.tls.enabled"))
                .isEqualTo("${RPC_TLS_ENABLED:true}");
        assertThat(properties("application-test.yml").getProperty("app.integrations.organization.version"))
                .endsWith(":1.0}");
    }

    @Test
    void binds_actual_rpc_ddc_and_http_registration_prefixes() {
        var binder = Binder.get(environment);
        var rpc = binder.bind("egon.cola.component.rpc", EgonRpcProperties.class).get();
        var ddc = binder.bind("egon.cola.component.tianshu", DdcProperties.class).get();
        var transport = binder.bind("egon.cola.component.tianshu.rpc", DdcRpcProperties.class).get();
        var http = binder.bind("egon.cola.component.tianshu.registry.http", DdcHttpRegistrationProperties.class).get();
        assertThat(rpc.isEnabled()).isFalse();
        assertThat(rpc.getProvider().getPort()).isEqualTo(50051);
        assertThat(ddc.getAppCode()).isEqualTo("egon-cola-source-service");
        assertThat(ddc.getEnv()).isEqualTo("test");
        assertThat(transport.getTls().isDevelopmentPlaintext()).isTrue();
        assertThat(http.isEnabled()).isFalse();
        assertThat(http.getServiceName()).isEqualTo("egon-cola-source-service");
    }

    private Properties properties(String path) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        var values = new Properties();
        factory.getObject().forEach((key, value) -> values.setProperty(key.toString(), value.toString()));
        return values;
    }

    private Set<String> keys(Properties properties) {
        return properties.stringPropertyNames().stream().filter(key -> key.startsWith("egon.cola.component.rpc.")
                || key.startsWith("egon.cola.component.tianshu.") || key.startsWith("egon.cola.component.yuheng.openapi.")
                || key.startsWith("egon.cola.platform.tianquan.shoubing.") || key.startsWith("app.integrations.organization."))
                .collect(Collectors.toSet());
    }
}
