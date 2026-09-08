package top.egon.cola.archetype.source.light.start.config;

import io.grpc.Status;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;

import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NativeRpcConfigurationTest {
    @Test
    void named_rpc_validation_reuses_the_existing_component_validator() {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(validators.getValidator());
            new ApplicationContextRunner().withUserConfiguration(NativeRpcConfiguration.class)
                    .withBean("egonColaValidationUtils", ValidationUtils.class, () -> validation)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean("nativeRpcValidation")).isSameAs(validation);
                        assertThat(context.getBean(ValidationUtils.class)).isSameAs(validation);
                        assertThat(context.getBean("lightRpcConverter")).isInstanceOf(LightRpcConverter.class);
                        var mapper = context.getBean("nativeRpcValidationExceptionMapper", RpcProviderExceptionMapper.class);
                        assertThat(mapper.map(new ConstraintViolationException(Set.of())).orElseThrow()
                                .getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                        assertThat(mapper.map(new DateTimeParseException("invalid", "bad", 0)).orElseThrow()
                                .getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                        assertThat(mapper.map(new IllegalStateException("server failure"))).isEmpty();
                    });
        }
    }

    @Test
    void all_profiles_define_the_same_native_keys_and_test_disables_external_lifecycles() {
        var base = properties("application.yml");
        assertThat(base.getProperty("spring.application.name")).isEqualTo("${APP_NAME:egon-cola-source-light}");
        for (String profile : new String[]{"dev", "test", "prod"}) {
            assertThat(nativeKeys(properties("application-" + profile + ".yml")))
                    .as(profile).isEqualTo(nativeKeys(base));
        }
        var test = properties("application-test.yml");
        for (String suffix : new String[]{"rpc.enabled", "rpc.provider.enabled", "rpc.consumer.enabled",
                "ddc.enabled", "ddc.registry.enabled", "ddc.redis.enabled", "ddc.registry.http.enabled",
                "gateway.openapi.enabled", "gateway.openapi.publish-to-ddc"}) {
            assertThat(test.getProperty("egon.cola.component." + suffix)).as(suffix).isEqualTo("false");
        }
        assertThat(test.getProperty("egon.cola.platform.idp.enabled")).isEqualTo("false");
        var prod = properties("application-prod.yml");
        assertThat(prod.getProperty("egon.cola.component.rpc.tls.enabled")).isEqualTo("${RPC_TLS_ENABLED:true}");
        assertThat(prod.getProperty("egon.cola.component.ddc.rpc.tls.enabled")).isEqualTo("${DDC_RPC_TLS_ENABLED:true}");
        assertThat(prod.getProperty("egon.cola.platform.idp.register-filter")).isEqualTo("false");
    }

    @Test
    void native_properties_bind_to_the_actual_component_prefixes() {
        var environment = new org.springframework.core.env.StandardEnvironment();
        var values = new java.util.LinkedHashMap<String, Object>();
        properties("application.yml").forEach((key, value) -> values.put(key.toString(), value));
        properties("application-test.yml").forEach((key, value) -> values.put(key.toString(), value));
        values.put("server.port", "8181");
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("native", values));
        var binder = org.springframework.boot.context.properties.bind.Binder.get(environment);
        var rpc = binder.bind("egon.cola.component.rpc", top.egon.cola.component.rpc.config.EgonRpcProperties.class).get();
        assertThat(rpc.isEnabled()).isFalse();
        assertThat(rpc.getProvider().getPort()).isEqualTo(50051);
        assertThat(rpc.getConsumer().getDefaultTimeoutMs()).isEqualTo(3000);
        var ddc = binder.bind("egon.cola.component.ddc", top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties.class).get();
        assertThat(ddc.getAppCode()).isEqualTo("egon-cola-source-light");
        assertThat(ddc.getEnv()).isEqualTo("test");
        assertThat(ddc.getRegistry().isEnabled()).isFalse();
        var transport = binder.bind("egon.cola.component.ddc.rpc", top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties.class).get();
        assertThat(transport.getTls().isDevelopmentPlaintext()).isTrue();
        var registration = binder.bind("egon.cola.component.ddc.registry.http", top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationProperties.class).get();
        assertThat(registration.getServiceName()).isEqualTo("egon-cola-source-light");
        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getPort()).isEqualTo(8181);
        var idp = binder.bind("egon.cola.platform.idp", top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties.class).get();
        assertThat(idp.isEnabled()).isFalse();
        assertThat(idp.isRegisterFilter()).isFalse();
        assertThat(idp.getServiceClient().getRegistrationId()).isEqualTo("ddcregistration");
    }

    private Properties properties(String path) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        var values = new Properties();
        factory.getObject().forEach((key, value) -> values.setProperty(key.toString(), value.toString()));
        return values;
    }

    private Set<String> nativeKeys(Properties values) {
        return values.stringPropertyNames().stream().filter(key -> key.startsWith("egon.cola.component.rpc.")
                || key.startsWith("egon.cola.component.ddc.")
                || key.startsWith("egon.cola.component.gateway.openapi.")
                || key.startsWith("egon.cola.platform.idp.")).collect(Collectors.toSet());
    }
}
