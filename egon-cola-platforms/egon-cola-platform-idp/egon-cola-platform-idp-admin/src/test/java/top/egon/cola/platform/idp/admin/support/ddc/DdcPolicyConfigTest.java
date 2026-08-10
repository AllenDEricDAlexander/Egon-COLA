package top.egon.cola.platform.idp.admin.support.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.service.refresh.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcAutoConfiguration;
import top.egon.cola.component.rpc.ddc.client.config.RpcDdcConfigClient;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DdcPolicyConfigTest {

    private static final Set<String> EXPECTED_EXPRESSIONS = Set.of(
            "${idp.token.access-ttl:900}",
            "${idp.token.refresh-ttl:604800}",
            "${idp.authorization-code.ttl:60}",
            "${idp.login.max-failures:5}",
            "${idp.login.lock-duration:900}",
            "${idp.password.max-concurrency:8}"
    );

    @Test
    void declaresOnlySixNonSecretRuntimePolicyKeys() {
        Set<String> expressions = new HashSet<>();

        for (Field field : IdpDdcValueDeclarations.class.getDeclaredFields()) {
            DdcValue value = field.getAnnotation(DdcValue.class);
            if (value != null) {
                expressions.add(value.value());
                assertFalse(value.refreshable());
                assertFalse(value.value().matches(
                        ".*(password-file|private-key|secret|credential).*"
                ));
            }
        }

        assertEquals(EXPECTED_EXPRESSIONS, expressions);
    }

    @Test
    void registersExactAppliersBeforeRegistryFreeze() throws Exception {
        DefaultDdcConfigApplierRegistry registry =
                new DefaultDdcConfigApplierRegistry((key, value, version) -> {
                    throw new AssertionError("IdP keys need exact appliers");
                });
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        InitializingBean registrar = new DdcPolicyConfig()
                .idpDdcPolicyRegistrar(registry, policy);

        registrar.afterPropertiesSet();
        registry.freeze();

        for (String key : AtomicIdpRuntimePolicy.CONFIG_KEYS) {
            assertInstanceOf(
                    IdpDdcPolicyApplier.class,
                    registry.resolve(key)
            );
        }
    }

    @Test
    void enabledDdcUsesTheDirectRpcConfigPort() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRpcAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.application.name=idp-admin-test",
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.biz-code=identity",
                        "egon.cola.component.ddc.env=test",
                        "egon.cola.component.ddc.app-code=idp-admin",
                        "egon.cola.component.ddc.rpc.target=dns:///127.0.0.1:19080",
                        "egon.cola.component.ddc.rpc.tls.development-plaintext=true",
                        "egon.cola.component.ddc.rpc.auth.runtime.access-key=test",
                        "egon.cola.component.ddc.rpc.auth.runtime.secret-key=test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context.getBean(DdcConfigClient.class))
                            .isInstanceOf(RpcDdcConfigClient.class);
                });
    }
}
